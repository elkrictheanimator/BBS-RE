package bbsfx.lyrics;

public final class LyricMotion {
   private LyricMotion() {
   }

   public static float unit(int a, long b, int salt, int seed) {
      int h = a * -1640531535;
      h ^= (int)(b ^ b >>> 32) * -2048144777;
      h ^= salt * -1028477379;
      h ^= seed * 668265263;
      h ^= h >>> 16;
      h *= -2048144789;
      h ^= h >>> 13;
      h *= -1028477387;
      h ^= h >>> 16;
      return (h >>> 8) / 1.6777216E7F;
   }

   public static float signed(int a, long b, int salt, int seed) {
      return unit(a, b, salt, seed) * 2.0F - 1.0F;
   }

   public static float clamp01(float x) {
      return x < 0.0F ? 0.0F : (x > 1.0F ? 1.0F : x);
   }

   public static float smoothstep(float x) {
      float t = clamp01(x);
      return t * t * (3.0F - 2.0F * t);
   }

   public static float approach(float from, float to, float age, float tau) {
      return age <= 0.0F ? from : to + (from - to) * (float)Math.exp(-age / Math.max(1.0F, tau));
   }

   public static float typeMillis(int chars, long lineMillis, float perChar, float share) {
      return Math.min(chars * perChar, (float)Math.max(1L, lineMillis) * share);
   }

   public static float lowestPitch(double distance, double margin) {
      double ratio = margin / Math.max(1.0E-4, distance);
      return (float)Math.toDegrees(Math.asin(Math.max(0.0, Math.min(1.0, ratio))));
   }

   public static double[] forward(double yaw, double pitch) {
      double y = Math.toRadians(yaw);
      double p = Math.toRadians(pitch);
      return new double[]{Math.sin(y) * Math.cos(p), -Math.sin(p), -Math.cos(y) * Math.cos(p)};
   }

   public static int visibleChars(String text) {
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
}
