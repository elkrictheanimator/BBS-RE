package mchorse.bbs_mod.ui.film;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.VertexSorter;
import mchorse.bbs_mod.camera.clips.misc.FrameBars;
import mchorse.bbs_mod.camera.data.Placement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Draws letterbox bars and full-screen colour transitions over the finished frame. */
public class UIFrameBarsRenderer
{
    public static void render(MatrixStack stack, Batcher2D batcher, List<FrameBars> bars)
    {
        if (bars.isEmpty())
        {
            return;
        }

        float width = UIImageRenderer.getUnitWidth();
        float height = Placement.HEIGHT;

        Matrix4f cache = new Matrix4f(RenderSystem.getProjectionMatrix());

        RenderSystem.setProjectionMatrix(new Matrix4f().ortho(0, width, height, 0, -100, 100), VertexSorter.BY_Z);
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (FrameBars bar : bars)
        {
            if (bar.ratio <= 0F)
            {
                if (Colors.getA(bar.color) > 0F)
                {
                    batcher.box(0, 0, width, height, bar.color);
                }

                continue;
            }

            float aspect = width / height;

            if (aspect > bar.ratio)
            {
                /* The frame is wider than the target: bars on the left and right (pillars)... */
                float side = (width - height * bar.ratio) / 2F * bar.factor;

                batcher.box(0, 0, side, height, bar.color);
                batcher.box(width - side, 0, width, height, bar.color);
            }
            else if (aspect < bar.ratio)
            {
                /* ...narrower: bars on top and bottom */
                float edge = (height - width / bar.ratio) / 2F * bar.factor;

                batcher.box(0, 0, width, edge, bar.color);
                batcher.box(0, height - edge, width, height, bar.color);
            }
        }

        RenderSystem.setProjectionMatrix(cache, VertexSorter.BY_Z);
        RenderSystem.enableCull();
    }
}
