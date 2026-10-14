package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.video.VideoPlayer;

/**
 * The video wallpaper: one looping video, played on the shared clock, drawn wherever a screen
 * asks for it. Every caller shares one decoder, so the landing screen and the editors behind it
 * never decode the same file twice.
 */
public class Wallpaper
{
    /** The one owner of the decoder; the video manager keeps one player per owner */
    private static final Object OWNER = new Object();

    private static final long STARTED = System.nanoTime();

    /** The veil over the video, so panels and text stay readable on a busy picture */
    private static final int VEIL = 0x66000000;

    /** The wallpaper's frame right now, or null when none is set or it hasn't decoded yet. */
    public static Texture frame()
    {
        return frame(OWNER, BBSSettings.backgroundVideo.get());
    }

    /**
     * The current frame of any video on the shared clock; each owner keeps its own decoder. The
     * landing banner plays its video this way, next to the wallpaper.
     */
    public static Texture frame(Object owner, Link link)
    {
        if (link == null)
        {
            return null;
        }

        VideoPlayer player = BBSModClient.getVideos().getPlayer(owner, link);

        if (player == null)
        {
            return null;
        }

        float seconds = (System.nanoTime() - STARTED) / 1_000_000_000F;
        float duration = player.getDuration();

        /* The length is not known until the file has been probed; until then the clock runs as is */
        if (duration > 0F)
        {
            seconds %= duration;
        }

        return player.getFrame(seconds);
    }

    /**
     * Draw the wallpaper over the rectangle, covering all of it and cropping what overflows.
     * Returns false when there is nothing to draw (no video, or no frame yet), so the caller
     * can fall back to what it showed before.
     */
    public static boolean draw(Batcher2D batcher, int x, int y, int w, int h)
    {
        Texture texture = frame();

        if (texture == null || texture.width <= 0 || texture.height <= 0 || w <= 0 || h <= 0)
        {
            return false;
        }

        float areaAspect = (float) w / h;
        float texAspect = (float) texture.width / texture.height;
        float cropW = texture.width;
        float cropH = texture.height;

        if (areaAspect > texAspect)
        {
            cropH = texture.width / areaAspect;
        }
        else
        {
            cropW = texture.height * areaAspect;
        }

        float u1 = (texture.width - cropW) * 0.5F;
        float v1 = (texture.height - cropH) * 0.5F;

        batcher.texturedBox(texture, Colors.WHITE, x, y, w, h, u1, v1, u1 + cropW, v1 + cropH, texture.width, texture.height);
        batcher.box(x, y, x + w, y + h, VEIL);

        return true;
    }
}
