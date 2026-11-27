package bbsfx.lyrics;

import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

public final class LyricClock {
   private LyricClock() {
   }

   public static float songTicks(AudioClip clip, int ticks, float transition) {
      return ticks - (Integer)clip.tick.get() + (Integer)clip.offset.get() + transition;
   }

   public static AudioClip sourceAudio(ClipContext context, Link wanted) {
      if (context.clips == null) {
         return null;
      }

      for (AudioClip clip : context.clips.getClips(AudioClip.class)) {
         int start = (Integer)clip.tick.get();
         if ((Boolean)clip.enabled.get()
            && context.ticks >= start
            && context.ticks < start + (Integer)clip.duration.get()
            && (wanted == null || wanted.equals(clip.audio.get()))) {
            return clip;
         }
      }

      return null;
   }

   public static float millis(ClipContext context, AudioClip audio, float offset) {
      float ticks = audio == null ? context.relativeTick + context.transition : songTicks(audio, context.ticks, context.transition);
      return ticks * 50.0F + offset;
   }

   public static double filmTicks(Clip lyricClip, AudioClip audio, float offset, double songMillis) {
      double ticks = (songMillis - offset) / 50.0;
      return audio == null ? ((Integer)lyricClip.tick.get()).intValue() + ticks : (Integer)audio.tick.get() - (Integer)audio.offset.get() + ticks;
   }

   public static Link lyricLink(ClipContext context, Link lrc, Link wantedAudio) {
      if (lrc != null) {
         return lrc;
      }

      Link audio = wantedAudio;
      if (audio == null) {
         AudioClip clip = sourceAudio(context, null);
         audio = clip == null ? null : (Link)clip.audio.get();
      }

      return Lrc.find(audio);
   }
}
