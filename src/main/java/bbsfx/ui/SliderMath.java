package bbsfx.ui;

public final class SliderMath {
   private SliderMath() {
   }

   public static double fraction(double x, double left, double right) {
      return !(right > left) ? 0.0 : clamp((x - left) / (right - left));
   }

   public static double at(double x, double left, double right, double min, double max) {
      return min + (max - min) * fraction(x, left, right);
   }

   public static double fractionOf(double value, double min, double max) {
      return max > min && Double.isFinite(min) && Double.isFinite(max) ? clamp((value - min) / (max - min)) : 0.0;
   }

   private static double clamp(double value) {
      return Math.max(0.0, Math.min(1.0, value));
   }
}
