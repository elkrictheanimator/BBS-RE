package bbsfx.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;

public final class LyricTrackSplitter {
   private LyricTrackSplitter() {
   }

   public static List<LyricTrackSplitter.Segment> split(MusicTextClip clip, List<Lrc.Line> lines, AudioClip audio, long tailMs) {
      if (clip != null && lines != null && !lines.isEmpty()) {
         int clipTick = (Integer)clip.tick.get();
         long first = clipTick;
         long last = first + Math.max(0, (Integer)clip.duration.get());
         List<LyricTrackSplitter.Segment> segments = new ArrayList();

         for (int i = 0; i < lines.size(); i++) {
            Lrc.Line line = (Lrc.Line)lines.get(i);
            long endMs = i + 1 < lines.size() ? ((Lrc.Line)lines.get(i + 1)).time : add(line.time, Math.max(0L, tailMs));
            long start = bound(clip, audio, line.time, first, last);
            long end = bound(clip, audio, endMs, first, last);
            if (!line.text.isBlank() && end > start) {
               segments.add(new LyricTrackSplitter.Segment((int)start, (int)(end - start), line.text, line.translation));
            }
         }

         return segments;
      } else {
         return Collections.emptyList();
      }
   }

   public static void assign(MusicTextClip clip, LyricTrackSplitter.Segment segment) {
      clip.tick.set(segment.tick());
      clip.duration.set(segment.duration());
      clip.title.set(segment.text());
      clip.segmentMode.set(true);
      clip.segmentText.set(segment.text());
      clip.segmentTranslation.set(segment.translation());
      clip.phrases.set("");
      clip.phraseLoop.set(false);
   }

   private static long bound(MusicTextClip clip, AudioClip audio, long songMs, long first, long last) {
      double ticks = LyricClock.filmTicks(clip, audio, (Float)clip.offset.get(), songMs);
      long rounded = Math.round(ticks);
      return Math.max(first, Math.min(last, rounded));
   }

   private static long add(long time, long tail) {
      return time > Long.MAX_VALUE - tail ? Long.MAX_VALUE : time + tail;
   }

   public record Segment(int tick, int duration, String text, String translation) {
      public Segment {
         translation = translation == null ? "" : translation;
      }
   }
}
