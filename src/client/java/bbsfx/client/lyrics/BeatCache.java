package bbsfx.client.lyrics;

import bbsfx.lyrics.BeatMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.audio.AudioReader;
import mchorse.bbs_mod.audio.Wave;
import mchorse.bbs_mod.resources.Link;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BeatCache {
   public static final float DECAY_MS = 110.0F;
   private static final Map<Link, BeatMap> MAPS = new ConcurrentHashMap();
   private static final Set<Link> LOADING = ConcurrentHashMap.newKeySet();
   private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "bbsfx-beats");
      thread.setDaemon(true);
      return thread;
   });

   private BeatCache() {
   }

   public static float pulse(Link audio, float millis) {
      if (audio != null && !(millis < 0.0F)) {
         BeatMap map = (BeatMap)MAPS.get(audio);
         if (map == null) {
            if (LOADING.add(audio)) {
               EXECUTOR.execute(() -> load(audio));
            }

            return 0.0F;
         } else {
            return map.pulse(millis, 110.0F);
         }
      } else {
         return 0.0F;
      }
   }

   public static float[] beats(Link audio) {
      if (audio == null) {
         return new float[0];
      }

      BeatMap map = (BeatMap)MAPS.get(audio);
      if (map == null) {
         if (LOADING.add(audio)) {
            EXECUTOR.execute(() -> load(audio));
         }

         return null;
      } else {
         float[] beats = new float[map.size()];

         for (int i = 0; i < beats.length; i++) {
            beats[i] = map.time(i);
         }

         return beats;
      }
   }

   public static void clear() {
      MAPS.clear();
   }

   private static void load(Link audio) {
      BeatMap map = BeatMap.EMPTY;

      try {
         map = analyse(AudioReader.read(BBSMod.getProvider(), audio));
      } catch (Throwable e) {
         System.err.println("[bbsfx] No beats for " + audio + ": " + e);
      } finally {
         MAPS.put(audio, map);
         LOADING.remove(audio);
      }
   }

   static BeatMap analyse(Wave wave) {
      if (wave != null && wave.data != null && wave.numChannels > 0) {
         if (wave.bitsPerSample != 16) {
            wave = wave.convertTo16();
         }

         byte[] data = wave.data;
         int channels = wave.numChannels;
         int frames = data.length / (2 * channels);
         BeatMap.Builder builder = new BeatMap.Builder(wave.sampleRate);

         for (int f = 0; f < frames; f++) {
            float mono = 0.0F;

            for (int c = 0; c < channels; c++) {
               int i = (f * channels + c) * 2;
               mono += (short)(data[i] & 0xFF | data[i + 1] << 8);
            }

            builder.add(mono / (channels * 32768.0F));
         }

         return builder.build();
      } else {
         return BeatMap.EMPTY;
      }
   }
}
