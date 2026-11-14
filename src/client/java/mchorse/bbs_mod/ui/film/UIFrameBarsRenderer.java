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
            if (bar.kind == 6)
            {
                /* Curved eyelids: the lids arch towards the middle, so they close like a real eye */
                float lid = height / 2F * bar.factor;
                float soft = height * 0.12F * bar.soft;
                int clear = bar.color & Colors.RGB;
                int slices = 120;
                float step = width / slices;

                for (int i = 0; i < slices; i++)
                {
                    float u = ((i + 0.5F) / slices) * 2F - 1F;
                    float edge = Math.min(height / 2F, lid * (1F + bar.curve * (1F - u * u)));
                    float x1 = i * step;
                    float x2 = i == slices - 1 ? width : x1 + step + 0.5F;
                    float body = Math.max(0F, edge - soft);

                    if (body > 0F)
                    {
                        batcher.box(x1, 0, x2, body, bar.color);
                        batcher.box(x1, height - body, x2, height, bar.color);
                    }

                    if (soft > 0.5F && edge < height / 2F - 0.5F)
                    {
                        batcher.gradientVBox(x1, body, x2, edge, bar.color, clear);
                        batcher.gradientVBox(x1, height - edge, x2, height - body, clear, bar.color);
                    }
                }

                continue;
            }

            if (bar.kind == 1)
            {
                /* Eyelids: two lids meet in the middle; a soft gradient edge */
                float lid = height / 2F * bar.factor;
                float soft = Math.min(lid, height * 0.15F * bar.soft);
                int clear = bar.color & Colors.RGB;

                batcher.box(0, 0, width, lid - soft, bar.color);
                batcher.box(0, height - lid + soft, width, height, bar.color);

                if (soft > 0.5F)
                {
                    batcher.gradientVBox(0, lid - soft, width, lid, bar.color, clear);
                    batcher.gradientVBox(0, height - lid, width, height - lid + soft, clear, bar.color);
                }

                continue;
            }

            if (bar.kind == 3)
            {
                /* CRT scanlines: every other thin band */
                if (Colors.getA(bar.color) > 0F)
                {
                    float band = Math.max(1.5F, height / (60F + 200F * (1F - bar.soft)));

                    for (float y = 0; y < height; y += band * 2F)
                    {
                        batcher.box(0, y, width, Math.min(height, y + band), bar.color);
                    }
                }

                continue;
            }

            if (bar.kind == 4)
            {
                if (Colors.getA(bar.color) > 0F)
                {
                    float t = Math.min(width, height) * 0.2F * bar.soft;

                    batcher.box(0, 0, width, t, bar.color);
                    batcher.box(0, height - t, width, height, bar.color);
                    batcher.box(0, t, t, height - t, bar.color);
                    batcher.box(width - t, t, width, height - t, bar.color);
                }

                continue;
            }

            if (bar.kind == 5)
            {
                if (Colors.getA(bar.color) > 0F)
                {
                    int rnd = bar.seed * 2654435 + 12345;
                    int glitches = 3 + (int) (bar.soft * 9F);

                    for (int i = 0; i < glitches; i++)
                    {
                        rnd = rnd * 1103515245 + 12345;
                        float y = ((rnd >>> 8) & 1023) / 1023F * height;
                        rnd = rnd * 1103515245 + 12345;
                        float h = (0.004F + ((rnd >>> 8) & 255) / 255F * 0.05F) * height;
                        rnd = rnd * 1103515245 + 12345;
                        float x = ((rnd >>> 8) & 255) / 255F * width * 0.5F;
                        float w = width * (0.3F + ((rnd >>> 16) & 255) / 255F * 0.7F);

                        batcher.box(x, y, Math.min(width, x + w), Math.min(height, y + h), bar.color);
                    }
                }

                continue;
            }

            if (bar.kind == 2)
            {
                if (Colors.getA(bar.color) > 0F)
                {
                    float edgeY = height * bar.soft;
                    float edgeX = Math.min(width, height) * bar.soft;
                    int clear = bar.color & Colors.RGB;

                    batcher.gradientVBox(0, 0, width, edgeY, bar.color, clear);
                    batcher.gradientVBox(0, height - edgeY, width, height, clear, bar.color);
                    batcher.gradientHBox(0, 0, edgeX, height, bar.color, clear);
                    batcher.gradientHBox(width - edgeX, 0, width, height, clear, bar.color);
                }

                continue;
            }

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
