package mchorse.bbs_mod.client;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.CameraWorkCameraController;
import mchorse.bbs_mod.camera.controller.ICameraController;

/** Whether the tracker clip that is on asks for the first-person hand or the HUD to stay visible. */
public class HandHudFlags
{
    private static boolean flag(int index)
    {
        ICameraController current = BBSModClient.getCameraController().getCurrent();

        if (current instanceof CameraWorkCameraController controller && controller.getContext() != null)
        {
            boolean[] flags = controller.getContext().clipData.get("handHud", () -> new boolean[2]);

            return flags[index];
        }

        return false;
    }

    public static boolean hand()
    {
        return flag(0);
    }

    public static boolean hud()
    {
        return flag(1);
    }
}
