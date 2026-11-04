package mchorse.bbs_mod.cubic.render.vao;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.cubic.data.model.ModelGroup;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.pose.ColorFx;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import org.joml.Vector3f;

/**
 * The colour effect of the bone being drawn, handed to the model shader as uniforms. A bone sets it
 * around its draw ({@link #set}), deferred translucent commands capture whatever is current when they
 * are queued and put it back for their replay, and {@link ModelVAORenderer#setupUniforms} uploads it.
 *
 * <p>Only shaders that declare {@code GradColor} (our model_dissolve) react; under a shader pack the
 * draw is plain, like any other mod-added uniform.</p>
 */
public final class ModelGradient
{
    /** Everything the shader needs for one draw. Axis/start/invLength are in the VAO's vertex space (blocks). */
    public static final class Params
    {
        public float r, g, b, strength;
        public float ax, ay, az;
        public float start, invLength;
        public float mode, style;
        public float speed, cycles, tiling, fps, frames;
        public int texture;
    }

    private static Params current;

    private ModelGradient()
    {}

    public static Params current()
    {
        return current;
    }

    public static void set(Params params)
    {
        current = params;
    }

    /** The effect of a bone for a draw, or null when it has none. formR/G/B = the form-level colour. */
    public static Params of(ModelGroup group, float formR, float formG, float formB)
    {
        ColorFx fx = group.fx;

        if (fx.strength <= 0F)
        {
            return null;
        }

        int axis = Math.max(0, Math.min(2, fx.axis));
        Vector3f pivot = group.initial.translate;
        float origin = fx.space == 1 ? 0F : (axis == 0 ? pivot.x : (axis == 1 ? pivot.y : pivot.z)) / 16F;
        float length = fx.length;

        /* A zero length is a hard edge: keep it a hair wide so the shader never divides by zero. */
        if (Math.abs(length) < 0.05F)
        {
            length = length < 0F ? -0.05F : 0.05F;
        }

        Params p = new Params();

        p.r = formR * fx.color2.r;
        p.g = formG * fx.color2.g;
        p.b = formB * fx.color2.b;
        p.strength = Math.min(1F, fx.strength);
        p.ax = axis == 0 ? 1F : 0F;
        p.ay = axis == 1 ? 1F : 0F;
        p.az = axis == 2 ? 1F : 0F;
        p.start = origin + fx.offset / 16F;
        p.invLength = 16F / length;
        p.mode = fx.mode == 1 ? 1F : 0F;
        p.style = fx.style;
        p.speed = fx.speed;
        p.cycles = fx.cycles;
        p.tiling = fx.tiling == 0F ? 1F : fx.tiling;
        p.fps = fx.fps;
        p.frames = Math.max(1, fx.frames);

        if (fx.style == ColorFx.STYLE_IMAGE)
        {
            Texture texture = fx.image.isEmpty() ? null : BBSModClient.getTextures().getTexture(Link.create(fx.image));

            if (texture == null)
            {
                /* No picture picked yet: fall back to the plain colour instead of sampling nothing. */
                p.style = ColorFx.STYLE_SOLID;
            }
            else
            {
                p.texture = texture.id;
            }
        }

        return p;
    }

    /** Put the effect's picture on unit 3 — before the samplers of the draw are read. */
    public static void bindImage()
    {
        Params p = current;

        if (p != null && p.style == ColorFx.STYLE_IMAGE)
        {
            RenderSystem.setShaderTexture(3, p.texture);
        }
    }

    /** Upload the current effect, or switch it off — uniforms persist in the program between draws. */
    public static void upload(ShaderProgram shader)
    {
        GlUniform color = shader.getUniform("GradColor");

        if (color == null)
        {
            return;
        }

        Params p = current;

        if (p == null)
        {
            color.set(0F, 0F, 0F, 0F);

            return;
        }

        color.set(p.r, p.g, p.b, p.strength);

        set(shader, "GradAxis", p.ax, p.ay, p.az);
        set(shader, "GradRange", p.start, p.invLength);
        set(shader, "GradMode", p.mode);
        set(shader, "GradStyle", p.style);
        set(shader, "GradTime", (System.currentTimeMillis() % 3_600_000L) / 1000F);
        set(shader, "GradAnim", p.speed, p.cycles, p.tiling, p.fps);
        set(shader, "GradFrames", p.frames);
    }

    private static void set(ShaderProgram shader, String name, float... values)
    {
        GlUniform uniform = shader.getUniform(name);

        if (uniform == null)
        {
            return;
        }

        switch (values.length)
        {
            case 1 -> uniform.set(values[0]);
            case 2 -> uniform.set(values[0], values[1]);
            case 3 -> uniform.set(values[0], values[1], values[2]);
            default -> uniform.set(values[0], values[1], values[2], values[3]);
        }
    }
}
