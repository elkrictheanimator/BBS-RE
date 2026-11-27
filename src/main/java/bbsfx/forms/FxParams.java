package bbsfx.forms;

public final class FxParams {
   private FxParams() {
   }

   public static int count(int value, int legacy) {
      return Math.max(3, legacy + value - 24);
   }

   public static float thickness(float value) {
      return value / 0.12F;
   }

   public static float mix(float value, float legacy) {
      return clamp01(legacy + (value - 0.5F) * 2.0F);
   }

   public static float fade(float alpha, float value) {
      return (float)Math.pow(Math.max(0.0F, alpha), value);
   }

   public static float life(float progress, boolean loop, float value) {
      if (loop || value == 1.0F) {
         return 1.0F;
      } else {
         return progress >= 1.0F ? 0.0F : (float)Math.pow(Math.max(0.0F, 1.0F - progress), value - 1.0F);
      }
   }

   public static float flicker(float value, float time, float speed) {
      if (!(value <= 0.0F) && value != 1.0F) {
         float amount = Math.min(0.45F, 0.1F + Math.abs(value - 1.0F) * 0.2F);
         return 1.0F - amount + amount * (float)Math.sin(time * 0.12F * speed * value);
      } else {
         return 1.0F;
      }
   }

   public static float layer(int layers, int index) {
      return layers <= 1 ? 0.0F : (float)index / (layers - 1);
   }

   public static float offset(int layers, int index, float height, float size) {
      return (index - (layers - 1) * 0.5F) * height * size * 0.2F;
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }
}
