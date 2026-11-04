package mchorse.bbs_mod.cubic.render.vao;

import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import org.joml.Vector3f;

/**
 * The colour gradient of the bone being drawn, handed to the model shader as uniforms. A bone sets it
 * around its draw ({@link #set}), deferred translucent commands capture whatever is current when they
 * are queued and put it back for their replay, and {@link ModelVAORenderer#setupUniforms} uploads it.
 *
 * <p>Layout of the array: r, g, b of the far colour (already multiplied by the form colour), strength,
 * axis x/y/z, range start and 1 / range length — both along the axis, in the vertex space of the VAO
 * (blocks, model rest pose). Only shaders that declare {@code GradColor} (our model_dissolve) react;
 * under a shader pack the draw is plain, like any other mod-added uniform.</p>
 */
public final class ModelGradient
{
    private static float[] current;

    private ModelGradient()
    {}

    public static float[] current()
    {
        return current;
    }

    public static void set(float[] gradient)
    {
        current = gradient;
    }

    /** The gradient of a bone for a draw, or null when it has none. formR/G/B = the form-level colour. */
    public static float[] of(ModelGroup group, float formR, float formG, float formB)
    {
        if (group.gradient <= 0F || Math.abs(group.gradientLength) < 0.001F)
        {
            return null;
        }

        int axis = Math.max(0, Math.min(2, group.gradientAxis));
        Vector3f pivot = group.initial.translate;
        float origin = (axis == 0 ? pivot.x : (axis == 1 ? pivot.y : pivot.z)) / 16F;
        float length = group.gradientLength / 16F;

        return new float[]
        {
            formR * group.gradientColor.r, formG * group.gradientColor.g, formB * group.gradientColor.b, Math.min(1F, group.gradient),
            axis == 0 ? 1F : 0F, axis == 1 ? 1F : 0F, axis == 2 ? 1F : 0F,
            origin, 1F / length
        };
    }

    /** Upload the current gradient, or switch it off — uniforms persist in the program between draws. */
    public static void upload(ShaderProgram shader)
    {
        GlUniform color = shader.getUniform("GradColor");

        if (color == null)
        {
            return;
        }

        float[] g = current;

        if (g == null)
        {
            color.set(0F, 0F, 0F, 0F);

            return;
        }

        color.set(g[0], g[1], g[2], g[3]);

        GlUniform axis = shader.getUniform("GradAxis");
        GlUniform range = shader.getUniform("GradRange");

        if (axis != null)
        {
            axis.set(g[4], g[5], g[6]);
        }

        if (range != null)
        {
            range.set(g[7], g[8]);
        }
    }
}
