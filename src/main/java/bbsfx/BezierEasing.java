package bbsfx;

public class BezierEasing {
   public static float eval(float progress, float x1, float y1, float x2, float y2) {
      if (progress <= 0.0F) {
         return 0.0F;
      }

      if (progress >= 1.0F) {
         return 1.0F;
      }

      float cx1 = clamp01(x1);
      float cx2 = clamp01(x2);
      if (cx1 == 0.0F && cx2 == 1.0F && y1 == 0.0F && y2 == 1.0F) {
         return progress;
      }

      float t = solveT(progress, cx1, cx2);
      return bezY(t, y1, y2);
   }

   private static float solveT(float x, float x1, float x2) {
      float t = x;

      for (int i = 0; i < 8; i++) {
         float currentX = bezX(t, x1, x2) - x;
         float derivative = dX(t, x1, x2);
         if (Math.abs(currentX) < 1.0E-5F) {
            return t;
         }

         if (Math.abs(derivative) < 1.0E-6F) {
            break;
         }

         t = clamp(t - currentX / derivative, 0.0F, 1.0F);
      }

      float low = 0.0F;
      float high = 1.0F;
      t = x;

      for (int i = 0; i < 24; i++) {
         float currentX = bezX(t, x1, x2);
         if (Math.abs(currentX - x) < 1.0E-5F) {
            break;
         }

         if (currentX < x) {
            low = t;
         } else {
            high = t;
         }

         t = (low + high) / 2.0F;
      }

      return t;
   }

   private static float bezX(float t, float x1, float x2) {
      float omt = 1.0F - t;
      return 3.0F * omt * omt * t * x1 + 3.0F * omt * t * t * x2 + t * t * t;
   }

   private static float bezY(float t, float y1, float y2) {
      float omt = 1.0F - t;
      return 3.0F * omt * omt * t * y1 + 3.0F * omt * t * t * y2 + t * t * t;
   }

   private static float dX(float t, float x1, float x2) {
      float omt = 1.0F - t;
      return 3.0F * omt * omt * x1 + 6.0F * omt * t * (x2 - x1) + 3.0F * t * t * (1.0F - x2);
   }

   public static float clamp01(float v) {
      return v < 0.0F ? 0.0F : (v > 1.0F ? 1.0F : v);
   }

   public static float clamp(float v, float min, float max) {
      return v < min ? min : (v > max ? max : v);
   }
}
