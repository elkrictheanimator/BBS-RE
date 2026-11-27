package bbsfx.lyrics;

import bbsfx.TypoMarkup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class LyricSplit {
   private static final int MIN_CHARS = 2;
   private static final long MIN_GAP_MS = 450L;
   private static final double SUNG = 0.85;
   private static final long CHAR_MS = 280L;
   private static final Map<Lrc, List<Lrc.Line>> SPLIT = new IdentityHashMap();
   private static final Map<Lrc, List<Lrc.Line>> WHOLE = new IdentityHashMap();

   private LyricSplit() {
   }

   public static synchronized void clearCache() {
      SPLIT.clear();
      WHOLE.clear();
   }

   public static synchronized List<Lrc.Line> view(Lrc lrc, boolean split) {
      Map<Lrc, List<Lrc.Line>> cache = split ? SPLIT : WHOLE;
      List<Lrc.Line> cached = (List<Lrc.Line>)cache.get(lrc);
      if (cached != null) {
         return cached;
      }

      List<Lrc.Line> kept = new ArrayList();

      for (Lrc.Line line : lrc.lines) {
         if (!Lrc.isCredit(line.text) && !Lrc.isPlaceholder(line.text)) {
            kept.add(line);
         }
      }

      List<Lrc.Line> lines = Collections.unmodifiableList(split ? split(kept) : kept);
      cache.put(lrc, lines);
      return lines;
   }

   public static int indexAt(List<Lrc.Line> lines, float millis) {
      int low = 0;
      int high = lines.size() - 1;
      int found = -1;

      while (low <= high) {
         int middle = low + high >>> 1;
         if ((float)((Lrc.Line)lines.get(middle)).time <= millis) {
            found = middle;
            low = middle + 1;
         } else {
            high = middle - 1;
         }
      }

      return found;
   }

   public static List<Lrc.Line> split(List<Lrc.Line> lines) {
      List<Lrc.Line> out = new ArrayList(lines.size());

      for (int i = 0; i < lines.size(); i++) {
         Lrc.Line line = (Lrc.Line)lines.get(i);
         long end = i + 1 < lines.size() ? ((Lrc.Line)lines.get(i + 1)).time : -1L;
         out.addAll(phrases(line, end));
      }

      out.sort((a, b) -> Long.compare(a.time, b.time));
      return out;
   }

   static List<Integer> cuts(String text) {
      List<Integer> cuts = new ArrayList();
      int i = 0;

      while (i < text.length()) {
         char c = text.charAt(i);
         if (c != ' ' && c != 12288) {
            i++;
         } else {
            int start = i;
            boolean ideographic = false;

            while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == 12288)) {
               ideographic |= text.charAt(i) == 12288;
               i++;
            }

            if (start != 0 && i < text.length()) {
               int before = text.codePointBefore(start);
               int after = text.codePointAt(i);
               if (ideographic || TypoMarkup.isWide(before) && TypoMarkup.isWide(after)) {
                  cuts.add(i);
               }
            }
         }
      }

      return cuts;
   }

   private static List<Lrc.Line> phrases(Lrc.Line line, long end) {
      String joined = line.words == null ? line.text : join(line.words);
      List<Integer> cuts = cuts(joined);
      if (cuts.isEmpty()) {
         return Collections.singletonList(line);
      }

      List<LyricSplit.Phrase> phrases = new ArrayList();
      int from = 0;

      for (int k = 0; k <= cuts.size(); k++) {
         int to = k < cuts.size() ? (Integer)cuts.get(k) : joined.length();
         phrases.add(new LyricSplit.Phrase(from, to));
         from = to;
      }

      if (line.words != null) {
         timeByWords(line, phrases);
      } else {
         timeByShare(line, joined, end, phrases);
      }

      List<LyricSplit.Phrase> kept = new ArrayList();

      for (LyricSplit.Phrase phrase : phrases) {
         LyricSplit.Phrase last = kept.isEmpty() ? null : (LyricSplit.Phrase)kept.get(kept.size() - 1);
         int chars = visible(joined.substring(phrase.from, phrase.to));
         if (last == null || chars >= 2 && visible(joined.substring(last.from, last.to)) >= 2 && phrase.start - last.start >= 450L) {
            kept.add(phrase);
         } else {
            last.absorb(phrase);
         }
      }

      if (kept.size() > 1 && end >= 0L && end - ((LyricSplit.Phrase)kept.get(kept.size() - 1)).start < 450L) {
         LyricSplit.Phrase tail = (LyricSplit.Phrase)kept.remove(kept.size() - 1);
         ((LyricSplit.Phrase)kept.get(kept.size() - 1)).absorb(tail);
      }

      if (kept.size() <= 1) {
         return Collections.singletonList(line);
      }

      List<Lrc.Line> out = new ArrayList();

      for (LyricSplit.Phrase phrase : kept) {
         String text = joined.substring(phrase.from, phrase.to).trim();
         out.add(new Lrc.Line(phrase.start, text, phrase.words, out.isEmpty() ? line.translation : null));
      }

      return out;
   }

   private static void timeByWords(Lrc.Line line, List<LyricSplit.Phrase> phrases) {
      int offset = 0;

      for (Lrc.Word word : line.words) {
         int lead = 0;

         while (lead < word.text.length() && Character.isWhitespace(word.text.charAt(lead))) {
            lead++;
         }

         int at = offset + Math.min(lead, Math.max(0, word.text.length() - 1));
         LyricSplit.Phrase owner = (LyricSplit.Phrase)phrases.get(0);

         for (LyricSplit.Phrase phrase : phrases) {
            if (phrase.from <= at) {
               owner = phrase;
            }
         }

         owner.words.add(word);
         if (lead < word.text.length()) {
            owner.start = Math.min(owner.start, word.time);
         }

         offset += word.text.length();
      }

      for (LyricSplit.Phrase phrase : phrases) {
         if (phrase.start == Long.MAX_VALUE) {
            phrase.start = line.time;
         }
      }
   }

   private static void timeByShare(Lrc.Line line, String text, long end, List<LyricSplit.Phrase> phrases) {
      int total = Math.max(1, visible(text));
      long length = end >= 0L ? Math.max(0L, end - line.time) : total * 280L;
      double sung = length * 0.85;
      int before = 0;

      for (LyricSplit.Phrase phrase : phrases) {
         phrase.start = line.time + Math.round(sung * before / total);
         phrase.words = null;
         before += visible(text.substring(phrase.from, phrase.to));
      }
   }

   private static String join(List<Lrc.Word> words) {
      StringBuilder builder = new StringBuilder();

      for (Lrc.Word word : words) {
         builder.append(word.text);
      }

      return builder.toString();
   }

   private static int visible(String text) {
      int count = 0;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) {
            count++;
         }

         i += Character.charCount(codePoint);
      }

      return count;
   }

   private static final class Phrase {
      final int from;
      int to;
      long start = Long.MAX_VALUE;
      List<Lrc.Word> words = new ArrayList();

      Phrase(int from, int to) {
         this.from = from;
         this.to = to;
      }

      void absorb(LyricSplit.Phrase next) {
         this.to = next.to;
         if (this.words != null && next.words != null) {
            this.words.addAll(next.words);
         }
      }
   }
}
