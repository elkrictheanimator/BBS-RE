package bbsfx;

public final class ChatColorCodes {
   public static final int NO_COLOR = -1;
   public static final float OUTLINE_SHADE = 0.4F;
   public static final float SHADOW_SHADE = 0.25F;
   private static final int[] COLORS = new int[]{
      0, 170, 43520, 43690, 11141120, 11141290, 16755200, 11184810, 5592405, 5592575, 5635925, 5636095, 16733525, 16733695, 16777045, 16777215
   };

   private ChatColorCodes() {
   }

   public static int rgb(char code) {
      char c = Character.toLowerCase(code);
      if (c >= '0' && c <= '9') {
         return COLORS[c - 48];
      } else {
         return c >= 97 && c <= 102 ? COLORS[10 + (c - 97)] : -1;
      }
   }

   public static int shade(int rgb, float factor) {
      int r = Math.round((rgb >> 16 & 0xFF) * factor);
      int g = Math.round((rgb >> 8 & 0xFF) * factor);
      int b = Math.round((rgb & 0xFF) * factor);
      return Math.min(255, r) << 16 | Math.min(255, g) << 8 | Math.min(255, b);
   }

   public static boolean isReset(char code) {
      return Character.toLowerCase(code) == 'r';
   }

   public static boolean isFormatting(char code) {
      char c = Character.toLowerCase(code);
      return c >= 'k' && c <= 'o';
   }
}
