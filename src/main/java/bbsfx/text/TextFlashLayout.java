package bbsfx.text;

import bbsfx.lyrics.Lrc;
import bbsfx.lyrics.LyricMotion;
import bbsfx.lyrics.LyricWords;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;

public final class TextFlashLayout {
   public static final long SLAM_MS = 260L;
   public static final long FADE_MS = 150L;
   public static final int MAX_ROWS = 4;
   public static final float SIZE_TAU_MS = 70.0F;
   public static final float POP_TAU_MS = 45.0F;
   public static final long WORD_STEP_MS = 380L;

   private TextFlashLayout() {
   }

   public static TextFlashLayout.Block build(Lrc.Line line, long end, TextFlashLayout.Style style) {
      List<TextFlashLayout.Piece> pieces = new ArrayList();
      int i = 0;

      for (LyricWords.Unit unit : LyricWords.units(line, end, 2, 380L)) {
         String text = style.uppercase ? unit.text.toUpperCase() : unit.text;
         boolean slam = LyricWords.length(text) >= 3 && LyricMotion.unit(i, line.time, 41, style.seed) < style.slamChance;
         pieces.add(new TextFlashLayout.Piece(text, unit.time, slam));
         i++;
      }

      int capacity = Math.max(2, style.rowLength);
      List<TextFlashLayout.Row> rows = new ArrayList();
      TextFlashLayout.Row row = null;
      int used = 0;

      for (TextFlashLayout.Piece piece : pieces) {
         int length = LyricWords.length(piece.text);
         boolean space = row != null && (!((TextFlashLayout.Piece)row.pieces.get(row.pieces.size() - 1)).wide || !piece.wide);
         int needed = used + (space ? 1 : 0) + length;
         boolean alone = length * 2 > capacity || used * 2 > capacity;
         if (row == null || alone || needed > capacity) {
            row = new TextFlashLayout.Row();
            rows.add(row);
            int var20 = false;
            needed = length;
         }

         row.pieces.add(piece);
         used = needed;
      }

      boolean red = LyricMotion.unit(0, line.time, 42, style.seed) < style.redChance;
      boolean glow = red || LyricMotion.unit(0, line.time, 43, style.seed) < style.glowChance;
      float tilt = LyricMotion.signed(0, line.time, 44, style.seed) * style.tilt;
      return new TextFlashLayout.Block(line.time, rows, red, glow, tilt);
   }

   public static float popScale(float ageMillis) {
      return ageMillis < 0.0F ? 1.0F : 1.0F + 0.26F * (float)Math.exp(-ageMillis / 45.0F);
   }

   public static float slamScale(float age) {
      float t = LyricMotion.clamp01(age / 260.0F);
      float land = 1.0F + 0.4F * (float)Math.exp(-Math.max(0.0F, age) / 40.0F);
      float leave = t > 0.7F ? 1.0F - (t - 0.7F) / 0.3F * 0.5F : 1.0F;
      return land * leave;
   }

   public static float slamAlpha(float age) {
      float t = LyricMotion.clamp01(age / 260.0F);
      return t > 0.8F ? 1.0F - (t - 0.8F) / 0.2F : 1.0F;
   }

   public static float trackedWidth(String text, float size, float tracking, ToDoubleFunction<Integer> advanceAtOne) {
      float width = 0.0F;
      int count = 0;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         width += (float)advanceAtOne.applyAsDouble(codePoint) * size;
         count++;
         i += Character.charCount(codePoint);
      }

      return width + Math.max(0, count - 1) * tracking * size;
   }

   public static float fitSize(double widthAtOne, float width) {
      float size = (float)(width / Math.max(0.01, widthAtOne));
      return Math.max(width * 0.15F, Math.min(width * 0.6F, size));
   }

   public static float rowSize(TextFlashLayout.Row row, float pos, ToDoubleFunction<String> widthAtOne, float width) {
      float size = Float.NaN;
      float target = 0.0F;
      long last = 0L;

      for (TextFlashLayout.Piece piece : row.pieces) {
         if ((float)piece.start > pos) {
            break;
         }

         if (!Float.isNaN(size) && piece.start == last) {
            target = fitSize(widthAtOne.applyAsDouble(row.text((float)piece.start)), width);
         } else {
            size = Float.isNaN(size) ? Float.NaN : LyricMotion.approach(size, target, (float)(piece.start - last), 70.0F);
            target = fitSize(widthAtOne.applyAsDouble(row.text((float)piece.start)), width);
            if (Float.isNaN(size)) {
               size = target;
            }

            last = piece.start;
         }
      }

      return Float.isNaN(size) ? 0.0F : LyricMotion.approach(size, target, pos - (float)last, 70.0F);
   }

   public static final class Block {
      public final long time;
      public final List<TextFlashLayout.Row> rows;
      public final boolean red;
      public final boolean glow;
      public final float tilt;

      Block(long time, List<TextFlashLayout.Row> rows, boolean red, boolean glow, float tilt) {
         this.time = time;
         this.rows = Collections.unmodifiableList(rows);
         this.red = red;
         this.glow = glow;
         this.tilt = tilt;
      }

      public TextFlashLayout.Piece slamming(float pos) {
         TextFlashLayout.Piece found = null;

         for (TextFlashLayout.Row row : this.rows) {
            for (TextFlashLayout.Piece piece : row.pieces) {
               if (piece.slam && (float)piece.start <= pos && pos - (float)piece.start < 260.0F) {
                  found = piece;
               }
            }
         }

         return found;
      }

      public List<TextFlashLayout.Row> visibleRows(float pos) {
         List<TextFlashLayout.Row> visible = new ArrayList();

         for (TextFlashLayout.Row row : this.rows) {
            if (row.visible(pos)) {
               visible.add(row);
            }
         }

         return visible.size() <= 4 ? visible : visible.subList(visible.size() - 4, visible.size());
      }

      public int pieceCount() {
         int count = 0;

         for (TextFlashLayout.Row row : this.rows) {
            count += row.pieces.size();
         }

         return count;
      }
   }

   public static final class Piece {
      public final String text;
      public final long start;
      public final boolean slam;
      public final boolean wide;

      public Piece(String text, long start, boolean slam) {
         this.text = text;
         this.start = start;
         this.slam = slam;
         this.wide = LyricWords.allWide(text);
      }
   }

   public static final class Row {
      public final List<TextFlashLayout.Piece> pieces = new ArrayList();

      public boolean visible(float pos) {
         return !this.pieces.isEmpty() && (float)((TextFlashLayout.Piece)this.pieces.get(0)).start <= pos;
      }

      public String text(float pos) {
         StringBuilder builder = new StringBuilder();
         TextFlashLayout.Piece previous = null;

         for (TextFlashLayout.Piece piece : this.pieces) {
            if ((float)piece.start > pos) {
               break;
            }

            if (previous != null && (!previous.wide || !piece.wide)) {
               builder.append(' ');
            }

            builder.append(piece.text);
            previous = piece;
         }

         return builder.toString();
      }

      public long newest(float pos) {
         long newest = Long.MIN_VALUE;

         for (TextFlashLayout.Piece piece : this.pieces) {
            if ((float)piece.start <= pos) {
               newest = Math.max(newest, piece.start);
            }
         }

         return newest;
      }
   }

   public static final class Style {
      public float slamChance = 0.22F;
      public float redChance = 0.32F;
      public float glowChance = 0.35F;
      public float tilt = 12.0F;
      public boolean uppercase = true;
      public int rowLength = 8;
      public int seed;
   }
}
