package mchorse.bbs_mod.utils.sodium;

import mchorse.bbs_mod.forms.renderers.utils.RecolorVertexConsumer;
import mchorse.bbs_mod.utils.colors.Color;
import net.minecraft.client.render.VertexConsumer;

/** Forge build: Sodium is not integrated. */
public class SodiumUtils
{
    public static VertexConsumer createVertexBuffer(VertexConsumer b, Color color)
    {
        return new RecolorVertexConsumer(b, color);
    }

    public static void disablePointCameraCulling()
    {}

    public static void restorePointCameraCulling()
    {}
}
