package bbsfx.lyrics;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;

public final class Lrc {
   private static final Map<String, Lrc> CACHE = new HashMap();
   private static final Map<String, Link> FIND_CACHE = new HashMap();
   private static final Set<String> FIND_MISS = new HashSet();
   private static final String[] EXTENSIONS = new String[]{".lrc", ".yrc"};
   private static final String[] TRANSLATION_SUFFIXES = new String[]{".tlrc", ".trans.lrc", ".translation.lrc", "-trans.lrc", "_trans.lrc", ".zh.lrc"};
   public static final long TRANSLATION_MATCH_MS = 1500L;
   public static final long TRANSLATION_STAMP_MS = 100L;
   private static final Pattern WORD_TAG = Pattern.compile("<(\\d+):(\\d+)(?:[.:](\\d+))?>");
   private static final Pattern YRC_LINE = Pattern.compile("^\\[(\\d+),(\\d+)\\]");
   private static final Pattern YRC_WORD = Pattern.compile("\\((\\d+),(\\d+)(?:,[^)]*)?\\)");
   private static final Pattern CREDIT = Pattern.compile(
      "^\\s*(作?词|作?曲|编曲|编配|制作人?|监制|混音|混缩|母带|录音|和声|和音|伴唱|吉他|贝斯|鼓|键盘|钢琴|弦乐|配唱|出品|发行|统筹|企划|策划|OP|SP|lyrics?(\\s+by)?|lyricist|written\\s+by|words\\s+by|compos(er|ed\\s+by)|music\\s+by|produc(er|ed\\s+by)|arrange(r|d\\s+by)|mix(ed\\s+by|ing)|master(ed\\s+by|ing))\\s*[:：]",
      2
   );
   public final List<Lrc.Line> lines = new ArrayList();
   private Map<String, Lrc> translations;
   private Lrc swapped;

   private static InputStream asset(Link link) {
      try {
         return BBSMod.getProvider().getAsset(link);
      } catch (Throwable unavailable) {
         return null;
      }
   }

   public static Lrc get(Link link) {
      if (link == null) {
         return null;
      }

      String key = link.toString();
      Lrc cached = (Lrc)CACHE.get(key);
      if (cached != null) {
         return cached;
      }

      Lrc lrc;
      try {
         InputStream stream = asset(link);

         label60: {
            Object var5;
            try {
               if (stream != null) {
                  lrc = parse(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
                  break label60;
               }

               var5 = null;
            } catch (Throwable var8) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var7) {
                     var8.addSuppressed(var7);
                  }
               }

               throw var8;
            }

            if (stream != null) {
               stream.close();
            }

            return (Lrc)var5;
         }

         if (stream != null) {
            stream.close();
         }
      } catch (Exception e) {
         return null;
      }

      CACHE.put(key, lrc);
      return lrc;
   }

   public static void cache(Link link, Lrc lrc) {
      CACHE.put(link.toString(), lrc);
   }

   public static Link find(Link audio) {
      if (audio != null && audio.path != null && !audio.path.isEmpty()) {
         String key = audio.toString();
         if (FIND_CACHE.containsKey(key)) {
            return (Link)FIND_CACHE.get(key);
         }

         if (FIND_MISS.contains(key)) {
            return null;
         }

         String name = audio.path;
         int slash = Math.max(name.lastIndexOf(47), name.lastIndexOf(92));
         if (slash >= 0) {
            name = name.substring(slash + 1);
         }

         int dot = name.lastIndexOf(46);
         if (dot >= 0) {
            name = name.substring(0, dot);
         }

         for (String extension : EXTENSIONS) {
            Link[] candidates = new Link[]{
               audio.parent().combine(name + extension), Link.assets("lyrics/" + name + extension), new Link(audio.source, "lyrics/" + name + extension)
            };

            for (Link candidate : candidates) {
               try {
                  InputStream stream = asset(candidate);

                  Link var15;
                  label72: {
                     try {
                        if (stream != null) {
                           FIND_CACHE.put(key, candidate);
                           var15 = candidate;
                           break label72;
                        }
                     } catch (Throwable var18) {
                        if (stream != null) {
                           try {
                              stream.close();
                           } catch (Throwable var17) {
                              var18.addSuppressed(var17);
                           }
                        }

                        throw var18;
                     }

                     if (stream != null) {
                        stream.close();
                     }
                     continue;
                  }

                  if (stream != null) {
                     stream.close();
                  }

                  return var15;
               } catch (Exception var19) {
               }
            }
         }

         FIND_MISS.add(key);
         return null;
      } else {
         return null;
      }
   }

   public static boolean isLyrics(String path) {
      String lower = path.toLowerCase();
      if (lower.endsWith(".tlrc")) {
         return true;
      }

      for (String extension : EXTENSIONS) {
         if (lower.endsWith(extension)) {
            return true;
         }
      }

      return false;
   }

   public static boolean isCredit(String text) {
      return CREDIT.matcher(text).find();
   }

   public static boolean isPlaceholder(String text) {
      StringBuilder letters = new StringBuilder();
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         if (Character.isLetterOrDigit(codePoint)) {
            letters.appendCodePoint(Character.toLowerCase(codePoint));
         }

         i += Character.charCount(codePoint);
      }

      String key = letters.toString();
      return key.startsWith("纯音乐") || key.contains("没有填词") || key.equals("instrumental");
   }

   public static void clearCache() {
      CACHE.clear();
      FIND_CACHE.clear();
      FIND_MISS.clear();
   }

   public static Lrc parse(String content) {
      Lrc lrc = new Lrc();
      lrc.read(content);
      return lrc;
   }

   private void read(String content) {
      long offset = 0L;
      List<Lrc.Line> parsed = new ArrayList();
      int lastTimed = -1;

      for (String raw : content.split("\\r?\\n")) {
         String line = raw.trim();
         if (!line.isEmpty() && line.charAt(0) != '{') {
            if (line.startsWith("[offset:")) {
               try {
                  offset = Long.parseLong(line.substring(8, line.length() - 1).trim());
               } catch (Exception var20) {
               }
            } else {
               Matcher yrc = YRC_LINE.matcher(line);
               if (yrc.find()) {
                  parsed.add(yrcLine(Long.parseLong(yrc.group(1)), line.substring(yrc.end())));
                  lastTimed = parsed.size() - 1;
               } else {
                  List<Long> times = new ArrayList();
                  int index = 0;

                  while (index < line.length() && line.charAt(index) == '[') {
                     int close = line.indexOf(93, index);
                     if (close < 0) {
                        break;
                     }

                     long time = parseTime(line.substring(index + 1, close));
                     if (time >= 0L) {
                        times.add(time);
                     }

                     index = close + 1;
                  }

                  String rest = line.substring(index);
                  String text = WORD_TAG.matcher(rest).replaceAll("").trim();
                  if (times.isEmpty()) {
                     if (lastTimed >= 0 && !text.isBlank() && !isCredit(text) && !isPlaceholder(text)) {
                        Lrc.Line owner = (Lrc.Line)parsed.get(lastTimed);
                        if (!owner.text.isBlank() && owner.translation == null) {
                           parsed.set(lastTimed, owner.translated(text));
                        }
                     }
                  } else {
                     Lrc.Line first = new Lrc.Line((Long)times.get(0), text, karaoke(rest, (Long)times.get(0)));

                     for (long time : times) {
                        parsed.add(first.shift(time - first.time));
                     }

                     lastTimed = parsed.size() - 1;
                  }
               }
            }
         }
      }

      for (Lrc.Line line : pairTranslations(parsed)) {
         this.lines.add(line.shift(-offset));
      }

      this.lines.sort((a, b) -> Long.compare(a.time, b.time));
   }

   private static List<Lrc.Line> pairTranslations(List<Lrc.Line> parsed) {
      List<Lrc.Line> out = new ArrayList(parsed.size());

      for (int i = 0; i < parsed.size(); i++) {
         Lrc.Line line = (Lrc.Line)parsed.get(i);

         while (i + 1 < parsed.size()) {
            Lrc.Line next = (Lrc.Line)parsed.get(i + 1);
            if (next.time < line.time || next.time - line.time > 100L || next.text.isBlank()) {
               break;
            }

            if (line.text.isBlank()) {
               if (line.time != next.time) {
                  break;
               }

               line = next;
               i++;
            } else {
               if (!next.text.equals(line.text) && line.translation == null) {
                  line = line.translated(next.text);
               }

               i++;
            }
         }

         out.add(line);
      }

      return out;
   }

   public synchronized Lrc translatedBy(Lrc other, String key) {
      if (this.translations == null) {
         this.translations = new HashMap();
      }

      return (Lrc)this.translations
         .computeIfAbsent(
            key,
            k -> {
               Lrc out = new Lrc();
               int j = 0;

               for (Lrc.Line line : this.lines) {
                  while (
                     j + 1 < other.lines.size()
                        && Math.abs(((Lrc.Line)other.lines.get(j + 1)).time - line.time) <= Math.abs(((Lrc.Line)other.lines.get(j)).time - line.time)
                  ) {
                     j++;
                  }

                  Lrc.Line match = other.lines.isEmpty() ? null : (Lrc.Line)other.lines.get(j);
                  boolean near = match != null && !match.text.isBlank() && Math.abs(match.time - line.time) <= 1500L;
                  out.lines.add(near && !line.text.isBlank() ? line.translated(match.text) : line);
               }

               return out;
            }
         );
   }

   public synchronized Lrc swapped() {
      if (this.swapped == null) {
         Lrc out = new Lrc();

         for (Lrc.Line line : this.lines) {
            out.lines.add(line.translation == null ? line : new Lrc.Line(line.time, line.translation, null, line.text));
         }

         this.swapped = out;
      }

      return this.swapped;
   }

   public boolean hasTranslation() {
      for (Lrc.Line line : this.lines) {
         if (line.translation != null) {
            return true;
         }
      }

      return false;
   }

   public static Link findTranslation(Link lyrics) {
      if (lyrics != null && lyrics.path != null && !lyrics.path.isEmpty()) {
         String key = "translation:" + lyrics;
         if (FIND_CACHE.containsKey(key)) {
            return (Link)FIND_CACHE.get(key);
         }

         if (FIND_MISS.contains(key)) {
            return null;
         }

         String name = lyrics.path;
         int slash = Math.max(name.lastIndexOf(47), name.lastIndexOf(92));
         int dot = name.lastIndexOf(46);
         if (dot > slash) {
            name = name.substring(slash + 1, dot);
         } else {
            name = name.substring(slash + 1);
         }

         for (String suffix : TRANSLATION_SUFFIXES) {
            Link candidate = lyrics.parent().combine(name + suffix);
            if (!candidate.equals(lyrics)) {
               try {
                  InputStream stream = asset(candidate);

                  Link var11;
                  label66: {
                     try {
                        if (stream != null) {
                           FIND_CACHE.put(key, candidate);
                           var11 = candidate;
                           break label66;
                        }
                     } catch (Throwable var14) {
                        if (stream != null) {
                           try {
                              stream.close();
                           } catch (Throwable var13) {
                              var14.addSuppressed(var13);
                           }
                        }

                        throw var14;
                     }

                     if (stream != null) {
                        stream.close();
                     }
                     continue;
                  }

                  if (stream != null) {
                     stream.close();
                  }

                  return var11;
               } catch (Exception var15) {
               }
            }
         }

         FIND_MISS.add(key);
         return null;
      } else {
         return null;
      }
   }

   private static List<Lrc.Word> karaoke(String rest, long lineTime) {
      Matcher matcher = WORD_TAG.matcher(rest);
      List<Lrc.Word> words = new ArrayList();
      int cursor = 0;
      long time = lineTime;

      boolean tagged;
      for (tagged = false; matcher.find(); tagged = true) {
         String piece = rest.substring(cursor, matcher.start());
         if (!piece.isEmpty() && (!words.isEmpty() || !piece.trim().isEmpty())) {
            words.add(new Lrc.Word(time, piece));
         }

         time = tagTime(matcher);
         cursor = matcher.end();
      }

      if (!tagged) {
         return null;
      }

      String tail = rest.substring(cursor);
      if (!tail.trim().isEmpty()) {
         words.add(new Lrc.Word(time, tail));
      }

      return words;
   }

   private static Lrc.Line yrcLine(long lineTime, String body) {
      Matcher matcher = YRC_WORD.matcher(body);
      List<Lrc.Word> words = new ArrayList();
      StringBuilder text = new StringBuilder();
      int cursor = -1;
      long time = lineTime;

      while (matcher.find()) {
         if (cursor >= 0) {
            addYrc(words, text, time, body.substring(cursor, matcher.start()));
         }

         time = Long.parseLong(matcher.group(1));
         cursor = matcher.end();
      }

      if (cursor < 0) {
         return new Lrc.Line(lineTime, body.trim(), null);
      }

      addYrc(words, text, time, body.substring(cursor));
      return new Lrc.Line(lineTime, text.toString().trim(), words);
   }

   private static void addYrc(List<Lrc.Word> words, StringBuilder text, long time, String piece) {
      if (!piece.isEmpty()) {
         words.add(new Lrc.Word(time, piece));
         text.append(piece);
      }
   }

   private static long tagTime(Matcher matcher) {
      long minutes = Long.parseLong(matcher.group(1));
      long seconds = Long.parseLong(matcher.group(2));
      return minutes * 60000L + seconds * 1000L + fraction(matcher.group(3));
   }

   private static long fraction(String digits) {
      if (digits != null && !digits.isEmpty()) {
         String ms = digits.length() > 3 ? digits.substring(0, 3) : digits;

         while (ms.length() < 3) {
            ms = ms + "0";
         }

         return Long.parseLong(ms);
      } else {
         return 0L;
      }
   }

   private static long parseTime(String tag) {
      int colon = tag.indexOf(58);
      if (colon < 0) {
         return -1L;
      }

      try {
         int minutes = Integer.parseInt(tag.substring(0, colon).trim());
         String rest = tag.substring(colon + 1).trim();
         int dot = Math.max(rest.indexOf(46), rest.indexOf(58));
         return dot >= 0
            ? minutes * 60000L + Integer.parseInt(rest.substring(0, dot)) * 1000L + fraction(rest.substring(dot + 1))
            : minutes * 60000L + Integer.parseInt(rest) * 1000L;
      } catch (Exception e) {
         return -1L;
      }
   }

   public int index(float millis) {
      int low = 0;
      int high = this.lines.size() - 1;
      int found = -1;

      while (low <= high) {
         int middle = low + high >>> 1;
         if ((float)((Lrc.Line)this.lines.get(middle)).time <= millis) {
            found = middle;
            low = middle + 1;
         } else {
            high = middle - 1;
         }
      }

      return found;
   }

   public long next(int index, long fallback) {
      return index + 1 < this.lines.size() ? ((Lrc.Line)this.lines.get(index + 1)).time : fallback;
   }

   public Lrc.Line at(float millis) {
      int index = this.index(millis);
      return index < 0 ? null : (Lrc.Line)this.lines.get(index);
   }

   public static final class Line {
      public final long time;
      public final String text;
      public final List<Lrc.Word> words;
      public final String translation;

      public Line(long time, String text, List<Lrc.Word> words) {
         this(time, text, words, null);
      }

      public Line(long time, String text, List<Lrc.Word> words, String translation) {
         this.time = time;
         this.text = text;
         this.words = words != null && !words.isEmpty() ? Collections.unmodifiableList(words) : null;
         this.translation = translation != null && !translation.isBlank() ? translation.trim() : null;
      }

      public Lrc.Line translated(String translation) {
         return new Lrc.Line(this.time, this.text, this.words, translation);
      }

      Lrc.Line shift(long delta) {
         if (delta == 0L) {
            return this;
         }

         List<Lrc.Word> moved = null;
         if (this.words != null) {
            moved = new ArrayList();

            for (Lrc.Word word : this.words) {
               moved.add(new Lrc.Word(Math.max(0L, word.time + delta), word.text));
            }
         }

         return new Lrc.Line(Math.max(0L, this.time + delta), this.text, moved, this.translation);
      }
   }

   public static final class Word {
      public final long time;
      public final String text;

      public Word(long time, String text) {
         this.time = time;
         this.text = text;
      }
   }
}
