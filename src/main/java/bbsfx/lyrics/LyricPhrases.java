package bbsfx.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LyricPhrases {
   public static final int POP = 0;
   public static final int FADE = 1;
   public static final int RISE = 2;
   public static final float INTRO_MS = 260.0F;

   private LyricPhrases() {
   }

   public static List<String> parse(String text) {
      if (text != null && !text.isBlank()) {
         List<String> lines = new ArrayList();

         for (String line : text.split("\\R")) {
            String phrase = line.trim();
            if (!phrase.isEmpty()) {
               lines.add(phrase);
            }
         }

         return lines;
      } else {
         return Collections.emptyList();
      }
   }

   public static String join(List<Lrc.Line> lines) {
      StringBuilder text = new StringBuilder();

      for (Lrc.Line line : lines) {
         if (!line.text.isBlank()) {
            if (text.length() > 0) {
               text.append('\n');
            }

            text.append(line.text.trim());
         }
      }

      return text.toString();
   }

   public static List<Lrc.Line> view(String manual, List<Lrc.Line> lyrics, float now, float stepMs, boolean loop) {
      long step = Math.max(50L, Math.round(stepMs));
      List<String> entered = parse(manual);
      boolean custom = !entered.isEmpty();
      List<Lrc.Line> base;
      if (custom) {
         base = new ArrayList(entered.size());

         for (int i = 0; i < entered.size(); i++) {
            base.add(new Lrc.Line(i * step, (String)entered.get(i), null));
         }
      } else {
         base = lyrics == null ? Collections.emptyList() : lyrics;
      }

      if (!base.isEmpty() && Float.isFinite(now)) {
         if (!loop) {
            if (!custom) {
               return base;
            }

            List<Lrc.Line> once = new ArrayList(base);
            once.add(new Lrc.Line(((Lrc.Line)base.get(base.size() - 1)).time + step, "", null));
            return once;
         } else {
            long start = ((Lrc.Line)base.get(0)).time;
            long cycle = Math.max(1L, ((Lrc.Line)base.get(base.size() - 1)).time + step - start);
            if (now < (float)start) {
               return base;
            }

            long current = (long)Math.floor((now - (float)start) / (float)cycle);
            if (current >= 9223372036854775806L) {
               return base;
            }

            long firstCycle = Math.max(0L, current - 1L);
            List<Lrc.Line> view = new ArrayList(base.size() * 3);

            for (long repetition = firstCycle; repetition <= current + 1L; repetition++) {
               long shift = repetition * cycle;

               for (Lrc.Line line : base) {
                  view.add(line.shift(shift));
               }
            }

            return view;
         }
      } else {
         return Collections.emptyList();
      }
   }

   public static float alpha(int template, float elapsed) {
      return template == 2 ? 1.0F : progress(elapsed);
   }

   public static float scale(int template, float elapsed) {
      return template == 0 ? 0.55F + 0.45F * progress(elapsed) : 1.0F;
   }

   public static float offset(int template, float elapsed) {
      return template == 2 ? 1.0F - progress(elapsed) : 0.0F;
   }

   private static float progress(float elapsed) {
      if (!Float.isFinite(elapsed)) {
         return elapsed > 0.0F ? 1.0F : 0.0F;
      } else {
         return Math.max(0.0F, Math.min(1.0F, elapsed / 260.0F));
      }
   }
}
