package mchorse.bbs_mod.video;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.FFMpegUtils;
import org.lwjgl.opengl.GL11;

import org.lwjgl.opengl.GL13;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A video that plays by itself, in a loop, for backgrounds: the landing banner and the wallpaper.
 *
 * <p>Unlike {@link VideoPlayer} it has no timeline to follow. One ffmpeg process loops the file
 * forever at its own pace (so there is no seek, and no stall when it starts over), scaled down
 * to a size a background never needs more than, and a worker thread keeps the newest frame; the
 * render thread only uploads it. When nobody asks for frames for a few seconds the process is
 * shut down, and the next ask starts it again.</p>
 *
 * <p>Two callers asking for the same file share one decoder.</p>
 */
public class LoopVideo
{
    private static final Map<String, LoopVideo> VIDEOS = new HashMap<>();

    private static final Pattern SIZE_PATTERN = Pattern.compile("Video:.*?(\\d{2,5})x(\\d{2,5})");

    /* More than this is wasted on a background, and costs a weak PC dearly */
    private static final int MAX_WIDTH = 854;
    private static final Pattern FPS_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?) fps");
    private static final double MAX_FPS = 60;
    private static final double DEFAULT_FPS = 30;

    private static final long IDLE_NANOS = 3_000_000_000L;
    private static final long RETRY_NANOS = 5_000_000_000L;

    /* Exactly one of these is set: a file in the assets, or an address on the web */
    private final File file;
    private final String url;
    private final Object lock = new Object();

    private ByteBuffer latest;
    private int version;
    private int uploaded;
    private Texture texture;

    private volatile boolean running;
    private volatile boolean restart;
    private volatile long lastUsed;
    private volatile long failedAt;
    private volatile String error;

    /* The sound of the video: 0 keeps it silent (and undecoded) */
    private volatile float volume;
    private volatile boolean audioOn;

    /** The shared decoder of a video; null when the link doesn't point at a file. */
    public static LoopVideo get(Link link)
    {
        if (link == null)
        {
            return null;
        }

        File file = BBSMod.getProvider().getFile(link);

        if (file == null || !file.isFile())
        {
            return null;
        }

        return VIDEOS.computeIfAbsent(file.getAbsolutePath(), (path) -> new LoopVideo(file, null));
    }

    /** The shared decoder of a video: the web address when there is one, otherwise the file the link points at. */
    public static LoopVideo get(Link link, String url)
    {
        if (url == null || url.trim().isEmpty())
        {
            return get(link);
        }

        String address = url.trim();

        return VIDEOS.computeIfAbsent(address, (key) -> new LoopVideo(null, address));
    }

    public static Texture frame(Link link, String url)
    {
        return frame(link, url, 0F);
    }

    /** Same, and the video's sound plays at the given volume (0..1) while the frames are being asked for. */
    public static Texture frame(Link link, String url, float volume)
    {
        LoopVideo video = get(link, url);

        if (video == null)
        {
            return null;
        }

        video.volume = Math.max(0F, Math.min(1F, volume));

        return video.getFrame();
    }

    public static String problem(Link link, String url)
    {
        if (url == null || url.trim().isEmpty())
        {
            return problem(link);
        }

        return get(link, url).error;
    }

    /** The current frame of a video, or null while there is none yet (or the video can't be played). */
    public static Texture frame(Link link)
    {
        LoopVideo video = get(link);

        return video == null ? null : video.getFrame();
    }

    /** Why a video can't be played, or null when it plays (or hasn't had the chance to fail yet). */
    public static String problem(Link link)
    {
        if (link == null)
        {
            return null;
        }

        LoopVideo video = get(link);

        return video == null ? "Không tìm thấy file video" : video.error;
    }

    private LoopVideo(File file, String url)
    {
        this.file = file;
        this.url = url;
    }

    /** What ffmpeg opens: the file, the address, or (for YouTube and the like) the address yt-dlp digs out of the page. */
    private String resolveInput() throws Exception
    {
        if (this.file != null)
        {
            return this.file.getAbsolutePath();
        }

        String lower = this.url.toLowerCase();
        boolean page = lower.contains("youtube.com") || lower.contains("youtu.be") || lower.contains("twitch.tv") || lower.contains("vimeo.com") || lower.contains("bilibili.com");

        if (!page)
        {
            return this.url;
        }

        String ytdlp = BBSSettings.ytdlpPath.get().trim();
        ProcessBuilder builder = new ProcessBuilder(ytdlp.isEmpty() ? "yt-dlp" : ytdlp, "-g", "--no-playlist", "-f", "best[height<=720][vcodec!=none]/best[height<=720]/best", this.url);
        Process process;

        try
        {
            process = builder.start();
        }
        catch (Exception e)
        {
            this.error = "Link này cần yt-dlp: cài yt-dlp rồi thử lại (" + e.getMessage() + ")";

            return null;
        }

        String output;

        try (InputStream stream = process.getInputStream())
        {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
        finally
        {
            process.destroy();
        }

        if (output.isEmpty())
        {
            this.error = "yt-dlp không lấy được video từ link này (link sai, riêng tư, hoặc yt-dlp cũ cần cập nhật)";

            return null;
        }

        return output.split("\\r?\\n")[0];
    }

    /** Render thread only: the newest decoded frame, uploaded. Starts the decoder when it is not running. */
    public Texture getFrame()
    {
        long now = System.nanoTime();

        this.lastUsed = now;

        /* Sound switched on while the decoder runs without it: start over, with it */
        if (this.running && this.volume > 0F && !this.audioOn)
        {
            this.restart = true;
        }

        if (!this.running && (this.error == null || now - this.failedAt > RETRY_NANOS))
        {
            this.start();
        }

        synchronized (this.lock)
        {
            if (this.latest != null && this.version != this.uploaded)
            {
                this.upload();
                this.uploaded = this.version;
            }
        }

        return this.texture;
    }

    private void start()
    {
        this.running = true;
        this.restart = false;
        this.error = null;

        /* Decided here, before the slow part (probing, yt-dlp), so asking for frames meanwhile
         * doesn't take the sound for "switched on later" and restart the decoder forever */
        this.audioOn = this.volume > 0F;

        Thread thread = new Thread(this::run, "BBS Background Video");

        thread.setDaemon(true);
        thread.start();
    }

    private void upload()
    {
        if (this.texture == null)
        {
            this.texture = new Texture();

            this.texture.setFilter(GL11.GL_LINEAR);
            this.texture.setWrap(GL13.GL_CLAMP_TO_EDGE);
        }

        this.texture.bind();
        this.texture.uploadTexture(GL11.GL_TEXTURE_2D, 0, this.width, this.height, this.latest);
        this.texture.unbind();
    }

    private int width;
    private double fps = DEFAULT_FPS;
    private int height;

    private void run()
    {
        Process process = null;
        Process audioProcess = null;

        try
        {
            String ffmpeg = FFMpegUtils.getFFMPEG();
            String input = this.resolveInput();

            if (input == null)
            {
                return;
            }

            int[] size = this.probe(ffmpeg, input);

            if (size == null)
            {
                return;
            }

            int w = Math.max(2, Math.min(size[0], MAX_WIDTH) & ~1);
            int h = Math.max(2, (int) Math.round(size[1] * (w / (double) size[0])) & ~1);

            ByteBuffer scratch = ByteBuffer.allocateDirect(w * h * 4);

            synchronized (this.lock)
            {
                if (this.latest == null || this.width != w || this.height != h)
                {
                    this.latest = ByteBuffer.allocateDirect(w * h * 4);
                    this.width = w;
                    this.height = h;
                    this.version = 0;
                    this.uploaded = 0;
                }
            }

            ProcessBuilder builder = new ProcessBuilder(
                ffmpeg, "-hide_banner", "-loglevel", "quiet", "-nostdin",
                "-re", "-stream_loop", "-1", "-i", input,
                "-an", "-sn", "-dn",
                "-vf", "fps=" + this.fps + ",scale=" + w + ":" + h + ":flags=fast_bilinear",
                "-f", "rawvideo", "-pix_fmt", "rgba", "pipe:1"
            );

            builder.redirectError(ProcessBuilder.Redirect.DISCARD);

            Process started = builder.start();

            process = started;

            /* The sound is its own process, started at the same moment and paced by the same clock:
             * a video without sound just makes it end at once, and the picture never waits for it */
            if (this.audioOn)
            {
                ProcessBuilder sound = new ProcessBuilder(
                    ffmpeg, "-hide_banner", "-loglevel", "quiet", "-nostdin",
                    "-re", "-stream_loop", "-1", "-i", input,
                    "-vn", "-sn", "-dn", "-map", "0:a:0?",
                    "-ac", "2", "-ar", "44100", "-f", "s16le", "pipe:1"
                );

                sound.redirectError(ProcessBuilder.Redirect.DISCARD);

                Process soundProcess = sound.start();

                audioProcess = soundProcess;

                Thread thread = new Thread(() -> this.playSound(soundProcess.getInputStream()), "BBS Background Video Sound");

                thread.setDaemon(true);
                thread.start();
            }

            ReadableByteChannel channel = Channels.newChannel(process.getInputStream());
            int frames = 0;

            while (!this.restart && System.nanoTime() - this.lastUsed < IDLE_NANOS)
            {
                scratch.clear();

                while (scratch.hasRemaining())
                {
                    if (channel.read(scratch) < 0)
                    {
                        if (frames == 0)
                        {
                            this.error = "ffmpeg không giải mã được video này (thử file .mp4 H.264)";
                        }

                        return;
                    }
                }

                scratch.flip();

                synchronized (this.lock)
                {
                    this.latest.clear();
                    this.latest.put(scratch);
                    this.latest.flip();
                    this.version++;
                }

                frames++;
            }
        }
        catch (Exception e)
        {
            this.error = "Không chạy được ffmpeg (đặt đường dẫn ffmpeg trong Settings > Video): " + e.getMessage();
        }
        finally
        {
            if (process != null)
            {
                process.destroy();
            }

            if (audioProcess != null)
            {
                audioProcess.destroy();
            }

            this.audioOn = false;

            if (this.error != null)
            {
                this.failedAt = System.nanoTime();
            }

            this.running = false;
        }
    }

    /**
     * Plays the raw sound ffmpeg sends until the stream ends (the process being killed closes it).
     * A reader keeps the pipe empty and hands the sound over through a queue, so a slow sound
     * device can never hold ffmpeg back and make the picture stall.
     */
    private void playSound(InputStream stream)
    {
        BlockingQueue<byte[]> queue = new ArrayBlockingQueue<>(64);
        byte[] end = new byte[0];

        Thread reader = new Thread(() ->
        {
            try
            {
                byte[] bytes = new byte[8192];
                int read;

                while ((read = stream.read(bytes)) > 0)
                {
                    byte[] chunk = Arrays.copyOf(bytes, read - (read % 4));

                    if (chunk.length == 0)
                    {
                        continue;
                    }

                    while (!queue.offer(chunk))
                    {
                        queue.poll();
                    }
                }
            }
            catch (Exception e)
            {}
            finally
            {
                queue.clear();
                queue.offer(end);
            }
        }, "BBS Background Video Sound Reader");

        reader.setDaemon(true);
        reader.start();

        SourceDataLine line = null;

        try
        {
            AudioFormat format = new AudioFormat(44100F, 16, 2, true, false);

            line = AudioSystem.getSourceDataLine(format);
            line.open(format, 44100);
            line.start();

            while (true)
            {
                byte[] bytes = queue.take();

                if (bytes.length == 0)
                {
                    break;
                }

                float volume = this.volume;

                /* Volume is applied to the samples, so it follows the slider at once */
                for (int i = 0; i + 1 < bytes.length; i += 2)
                {
                    short sample = (short) ((bytes[i] & 0xFF) | (bytes[i + 1] << 8));

                    sample = (short) (sample * volume * volume);
                    bytes[i] = (byte) sample;
                    bytes[i + 1] = (byte) (sample >> 8);
                }

                line.write(bytes, 0, bytes.length);
            }
        }
        catch (Exception e)
        {
            /* No sound device, or none that takes this format: the picture goes on without it */
        }
        finally
        {
            if (line != null)
            {
                line.close();
            }
        }
    }

    /** The video's own size, read off ffmpeg's info output; null (with the reason in {@link #error}) when it can't be. */
    private int[] probe(String ffmpeg, String input) throws Exception
    {
        ProcessBuilder builder = new ProcessBuilder(ffmpeg, "-hide_banner", "-i", input);

        builder.redirectErrorStream(true);

        Process probe = builder.start();
        String output;

        try (InputStream stream = probe.getInputStream())
        {
            output = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        finally
        {
            probe.destroy();
        }

        /* Keep the video's own frame rate: forcing another one drops frames unevenly, which is the stutter */
        Matcher rate = FPS_PATTERN.matcher(output);

        this.fps = DEFAULT_FPS;

        if (rate.find())
        {
            double parsed = Double.parseDouble(rate.group(1));

            if (parsed >= 1)
            {
                this.fps = Math.min(MAX_FPS, parsed);
            }
        }

        Matcher size = SIZE_PATTERN.matcher(output);

        if (!size.find())
        {
            this.error = "ffmpeg không đọc được file video này";

            return null;
        }

        int w = Integer.parseInt(size.group(1));
        int h = Integer.parseInt(size.group(2));

        if (w <= 0 || h <= 0)
        {
            this.error = "Kích thước video không hợp lệ";

            return null;
        }

        return new int[] {w, h};
    }
}
