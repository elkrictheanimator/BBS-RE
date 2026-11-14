package bbsfx;

public final class TextAnimOrder {
   public static final int FORWARD = 0;
   public static final int REVERSE = 1;
   public static final int CENTER = 2;
   public static final int RANDOM = 3;

   private TextAnimOrder() {
   }

   public static float position(int index, int count, int order, int seed) {
      if (count <= 1) {
         return 0.0F;
      } else {
         int safeIndex = Math.max(0, Math.min(count - 1, index));
         float forward = (float)safeIndex / (count - 1);
         if (order == 1) {
            return 1.0F - forward;
         } else if (order == 2) {
            float center = (count - 1) * 0.5F;
            return Math.abs(safeIndex - center) / Math.max(1.0F, center);
         } else if (order == 3) {
            long value = (safeIndex + 1L) * -7046029254386353131L ^ seed;
            value ^= value >>> 30;
            value *= -4658895280553007687L;
            value ^= value >>> 27;
            value *= -7723592293110705685L;
            value ^= value >>> 31;
            return (float)(value & 16777215L) / 1.6777216E7F;
         } else {
            return forward;
         }
      }
   }
}
