package bbsfx;

public final class Wiggle {
   public static final float ROTATION = 8.0F;
   public static final float OFFSET_X = 0.8F;
   public static final float OFFSET_Y = 1.5F;
   public static final float SCALE = 0.1F;
   public float angle;
   public float dx;
   public float dy;
   public float scale = 1.0F;

   public static long frame(double seconds, int fps) {
      return (long)Math.floor(seconds * Math.max(1, fps));
   }

   public Wiggle set(int glyph, long frame, int seed, float amount) {
      if (amount <= 0.0F) {
         this.angle = this.dx = this.dy = 0.0F;
         this.scale = 1.0F;
         return this;
      } else {
         this.angle = rand(glyph, frame, seed, 1) * 8.0F * amount;
         this.dx = rand(glyph, frame, seed, 2) * 0.8F * amount;
         this.dy = rand(glyph, frame, seed, 3) * 1.5F * amount;
         this.scale = 1.0F + rand(glyph, frame, seed, 4) * 0.1F * amount;
         return this;
      }
   }

   public static float rand(int glyph, long frame, int seed, int channel) {
      int h = glyph * 374761393 + (int)frame * 668265263 + (int)(frame >>> 32) * 1274126177 + seed * 1442695041 + channel * 1597334677;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return (h >>> 8) / 1.6777216E7F * 2.0F - 1.0F;
   }
}
