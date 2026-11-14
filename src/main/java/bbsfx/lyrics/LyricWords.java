package bbsfx.lyrics;

import bbsfx.TypoMarkup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class LyricWords {
   private static final float SPREAD = 0.8F;

   private LyricWords() {
   }

   public static boolean allWide(String text) {
      if (text.isEmpty()) {
         return false;
      }

      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         if (!TypoMarkup.isWide(codePoint)) {
            return false;
         }

         i += Character.charCount(codePoint);
      }

      return true;
   }

   public static boolean isPunctuation(int codePoint) {
      if (codePoint != 39 && codePoint != 8217) {
         switch (Character.getType(codePoint)) {
            case 20:
            case 21:
            case 22:
            case 24:
            case 29:
            case 30:
               return true;
            case 23:
            case 25:
            case 26:
            case 27:
            case 28:
            default:
               return false;
         }
      } else {
         return false;
      }
   }

   public static String clean(String text) {
      int start = 0;
      int end = text.length();

      while (start < end && skip(text.codePointAt(start))) {
         start += Character.charCount(text.codePointAt(start));
      }

      while (end > start && skip(text.codePointBefore(end))) {
         end -= Character.charCount(text.codePointBefore(end));
      }

      return text.substring(start, end);
   }

   private static boolean skip(int codePoint) {
      return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint) || isPunctuation(codePoint);
   }

   public static int length(String text) {
      int length = 0;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         length += TypoMarkup.isWide(codePoint) ? 2 : 1;
         i += Character.charCount(codePoint);
      }

      return length;
   }

   public static List<LyricWords.Unit> units(Lrc.Line line, long end, int group, long stepMs) {
      List<LyricWords.Unit> units = new ArrayList();
      int size = Math.max(1, group);
      if (line.words != null) {
         StringBuilder run = new StringBuilder();
         long runTime = 0L;
         int runCount = 0;

         for (Lrc.Word word : line.words) {
            String text = clean(word.text);
            if (!text.isEmpty()) {
               boolean single = text.codePointCount(0, text.length()) == 1 && allWide(text);
               if (!single && runCount > 0) {
                  units.add(new LyricWords.Unit(run.toString(), runTime));
                  run.setLength(0);
                  runCount = 0;
               }

               if (!single) {
                  units.add(new LyricWords.Unit(text, word.time));
               } else {
                  if (runCount == 0) {
                     runTime = word.time;
                  }

                  run.append(text);
                  if (++runCount >= size) {
                     units.add(new LyricWords.Unit(run.toString(), runTime));
                     run.setLength(0);
                     runCount = 0;
                  }
               }
            }
         }

         if (runCount > 0) {
            units.add(new LyricWords.Unit(run.toString(), runTime));
         }

         return units;
      } else {
         List<String> tokens = tokens(line.text, size, null);
         long[] times = spread(line.time, end, tokens.size(), stepMs);

         for (int i = 0; i < tokens.size(); i++) {
            units.add(new LyricWords.Unit((String)tokens.get(i), times[i]));
         }

         return units;
      }
   }

   public static long[] charTimes(Lrc.Line line, long end, long stepMs) {
      String text = line.text;
      long[] times = new long[text.length()];
      if (line.words == null) {
         int[] owner = new int[text.length()];
         List<String> tokens = tokens(text, 2, owner);
         long[] starts = spread(line.time, end, tokens.size(), stepMs);
         int next = -1;

         for (int i = text.length() - 1; i >= 0; i--) {
            if (owner[i] >= 0) {
               next = owner[i];
            }

            times[i] = next < 0 ? line.time : starts[next];
         }

         return times;
      } else {
         StringBuilder joined = new StringBuilder();
         List<Long> stamps = new ArrayList();

         for (Lrc.Word word : line.words) {
            for (int i = 0; i < word.text.length(); i++) {
               joined.append(word.text.charAt(i));
               stamps.add(word.time);
            }
         }

         int start = 0;

         while (start < joined.length() && Character.isWhitespace(joined.charAt(start))) {
            start++;
         }

         for (int i = 0; i < times.length; i++) {
            times[i] = start + i < stamps.size() ? (Long)stamps.get(start + i) : line.time;
         }

         return times;
      }
   }

   private static long[] spread(long start, long end, int count, long stepMs) {
      long[] times = new long[count];
      float duration = (float)Math.max(300L, end - start);
      float span = Math.min(duration * 0.8F, count * (float)stepMs);

      for (int i = 0; i < count; i++) {
         times[i] = start + (long)(span * i / Math.max(1, count));
      }

      return times;
   }

   private static List<String> tokens(String text, int group, int[] owner) {
      List<String> tokens = new ArrayList();
      StringBuilder latin = new StringBuilder();
      StringBuilder wide = new StringBuilder();
      int wideCount = 0;
      if (owner != null) {
         Arrays.fill(owner, -1);
      }

      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         int width = Character.charCount(codePoint);
         boolean punctuation = isPunctuation(codePoint) || Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
         if ((punctuation || TypoMarkup.isWide(codePoint)) && latin.length() > 0) {
            tokens.add(latin.toString());
            latin.setLength(0);
         }

         if ((punctuation || !TypoMarkup.isWide(codePoint)) && wide.length() > 0) {
            tokens.add(wide.toString());
            wide.setLength(0);
            wideCount = 0;
         }

         if (!punctuation) {
            if (TypoMarkup.isWide(codePoint)) {
               wide.appendCodePoint(codePoint);
               wideCount++;
            } else {
               latin.appendCodePoint(codePoint);
            }

            if (owner != null) {
               for (int k = 0; k < width; k++) {
                  owner[i + k] = tokens.size();
               }
            }

            if (wideCount >= group) {
               tokens.add(wide.toString());
               wide.setLength(0);
               wideCount = 0;
            }
         }

         i += width;
      }

      if (latin.length() > 0) {
         tokens.add(latin.toString());
      }

      if (wide.length() > 0) {
         tokens.add(wide.toString());
      }

      return tokens;
   }

   public static final class Unit {
      public final String text;
      public final long time;

      public Unit(String text, long time) {
         this.text = text;
         this.time = time;
      }

      public boolean wide() {
         return LyricWords.allWide(this.text);
      }
   }
}
