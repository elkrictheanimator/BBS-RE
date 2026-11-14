package bbsfx;

import java.util.ArrayList;
import java.util.List;

public final class TypoMarkup {
   public static final int OBFUSCATED = 1;
   public static final int BOLD = 2;
   public static final int STRIKETHROUGH = 4;
   public static final int UNDERLINE = 8;
   public static final int ITALIC = 16;
   private static final String OBFUSCATE_LATIN = "ABCDEFGHJKLMNPQRSTUVWXYZabdeghkmnopqrsuvwxyz0123456789#$%&@";
   private static final String OBFUSCATE_WIDE = "永和天地玄黄宇宙洪荒日月盈昃辰宿列张寒来暑往秋收冬藏风云雷电";

   private TypoMarkup() {
   }

   public static int styleBit(char code) {
      switch (Character.toLowerCase(code)) {
         case 'k':
            return 1;
         case 'l':
            return 2;
         case 'm':
            return 4;
         case 'n':
            return 8;
         case 'o':
            return 16;
         default:
            return 0;
      }
   }

   public static List<List<TypoMarkup.Glyph>> parse(String text) {
      List<List<TypoMarkup.Glyph>> lines = new ArrayList();
      List<TypoMarkup.Glyph> line = new ArrayList();
      int rgb = -1;
      int style = 0;
      int length = text.length();
      int i = 0;

      while (i < length) {
         int codePoint = text.codePointAt(i);
         i += Character.charCount(codePoint);
         if (codePoint == 167) {
            if (i >= length) {
               break;
            }

            char code = text.charAt(i++);
            int color = ChatColorCodes.rgb(code);
            if (color != -1) {
               rgb = color;
               style = 0;
            } else if (ChatColorCodes.isReset(code)) {
               rgb = -1;
               style = 0;
            } else {
               style |= styleBit(code);
            }
         } else if (codePoint == 10) {
            lines.add(line);
            line = new ArrayList();
         } else if (!Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
            line.add(new TypoMarkup.Glyph(codePoint, rgb, style));
         }
      }

      lines.add(line);
      return lines;
   }

   public static String drawPrefix(int style) {
      return (style & 18) == 0 ? "" : ((style & 2) != 0 ? "§l" : "") + ((style & 16) != 0 ? "§o" : "");
   }

   public static int obfuscate(int codePoint, int seed) {
      if (Character.isSpaceChar(codePoint)) {
         return codePoint;
      }

      String pool = isWide(codePoint) ? "永和天地玄黄宇宙洪荒日月盈昃辰宿列张寒来暑往秋收冬藏风云雷电" : "ABCDEFGHJKLMNPQRSTUVWXYZabdeghkmnopqrsuvwxyz0123456789#$%&@";
      int h = codePoint * 374761393 + seed * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return pool.codePointAt(Math.floorMod(h, pool.length()));
   }

   public static boolean isWide(int codePoint) {
      return codePoint >= 11904 && codePoint <= 42191
         || codePoint >= 44032 && codePoint <= 55215
         || codePoint >= 63744 && codePoint <= 64255
         || codePoint >= 65280 && codePoint <= 65519
         || codePoint >= 131072 && codePoint <= 262143;
   }

   public static final class Glyph {
      public final int codePoint;
      public final int rgb;
      public final int style;

      public Glyph(int codePoint, int rgb, int style) {
         this.codePoint = codePoint;
         this.rgb = rgb;
         this.style = style;
      }

      public boolean has(int bit) {
         return (this.style & bit) != 0;
      }

      public String text() {
         return new String(Character.toChars(this.codePoint));
      }
   }
}
