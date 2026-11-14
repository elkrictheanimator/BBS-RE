package bbsfx;

public final class TimeRemap {
   public static final int CLAMP = 0;
   public static final int LOOP = 1;
   public static final int PING_PONG = 2;

   private TimeRemap() {
   }

   public static float progress(float tick, float speed, float offset, float duration, int mode) {
      if (Float.isFinite(tick) && Float.isFinite(speed) && Float.isFinite(offset) && Float.isFinite(duration) && !(duration <= 0.0F)) {
         float value = (tick * speed + offset) / duration;
         if (!Float.isFinite(value)) {
            return 0.0F;
         } else if (mode == 1) {
            return positiveMod(value, 1.0F);
         } else if (mode == 2) {
            float phase = positiveMod(value, 2.0F);
            return phase <= 1.0F ? phase : 2.0F - phase;
         } else {
            return Math.max(0.0F, Math.min(1.0F, value));
         }
      } else {
         return 0.0F;
      }
   }

   private static float positiveMod(float value, float period) {
      float result = value % period;
      return result < 0.0F ? result + period : result;
   }
}
