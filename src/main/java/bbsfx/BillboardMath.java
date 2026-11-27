package bbsfx;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class BillboardMath {
   public static final int SCREEN_ALIGNED = 0;
   public static final int CAMERA_FACING = 1;
   public static final int WORLD_FIXED = 2;

   private BillboardMath() {
   }

   public static int orientation(boolean followScreen, boolean faceCamera) {
      if (!faceCamera) {
         return 2;
      } else {
         return followScreen ? 0 : 1;
      }
   }

   public static Vector3f screenToWorldOffset(Matrix4f view, Vector3f offset) {
      return new Matrix4f(view).invert().transformDirection(offset);
   }

   public static float yawTowardCamera(Matrix4f view, Vector3f viewOffset) {
      Vector3f world = new Matrix4f(view).invert().transformDirection(new Vector3f(viewOffset));
      float dx = -world.x;
      float dz = -world.z;
      return dx * dx + dz * dz < 1.0E-8F ? 0.0F : (float)Math.atan2(dx, dz);
   }

   public static Matrix4f yawRotation(Matrix4f view, Vector3f viewOffset) {
      return new Matrix4f(view).rotateY(yawTowardCamera(view, viewOffset));
   }
}
