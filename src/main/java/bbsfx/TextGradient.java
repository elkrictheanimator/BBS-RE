package bbsfx;

public final class TextGradient {
   public static final int HORIZONTAL = 0;
   public static final int VERTICAL = 1;

   private TextGradient() {
   }

   public static int lerp(int from, int to, float factor) {
      float f = Math.max(0.0F, Math.min(1.0F, factor));
      int r = Math.round((from >> 16 & 0xFF) * (1.0F - f) + (to >> 16 & 0xFF) * f);
      int g = Math.round((from >> 8 & 0xFF) * (1.0F - f) + (to >> 8 & 0xFF) * f);
      int b = Math.round((from & 0xFF) * (1.0F - f) + (to & 0xFF) * f);
      return r << 16 | g << 8 | b;
   }

   public static int at(int from, int to, int mode, float position, float span) {
      return mode == 1 ? lerp(from, to, span <= 0.0F ? 0.0F : position / span) : lerp(from, to, span <= 0.0F ? 1.0F : position / span);
   }
}
