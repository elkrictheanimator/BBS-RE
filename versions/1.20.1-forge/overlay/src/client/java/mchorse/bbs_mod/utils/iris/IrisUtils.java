package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import net.minecraft.client.render.BufferBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Forge build: Iris (and Oculus, its Forge port, which has a different API) is not integrated, so every
 * query answers "no shader pack" and the hooks do nothing.
 */
public class IrisUtils
{
    /** Stands in for Iris' cached uniform type, which the shader curves list is typed with. */
    public interface CachedUniform
    {}

    public static void setShaderProperties(Object shaderProperties)
    {}

    public static List<String> getSliderProperties()
    {
        return Collections.emptyList();
    }

    public static Map<String, String> getShadersLanguageMap(String language)
    {
        return Collections.emptyMap();
    }

    public static void setup()
    {}

    public static void trackPbrVariant(Texture variant, Link albedo, float smoothness, float metallic, float sss, float emission, float relief)
    {}

    public static void trackTexture(Texture texture)
    {}

    public static boolean isShaderPackEnabled()
    {
        return false;
    }

    public static void setMainBound(boolean bound)
    {}

    public static boolean shouldOverrideShaders()
    {
        return false;
    }

    public static void renderOffscreen(Runnable render)
    {
        render.run();
    }

    public static boolean isRenderingOffscreen()
    {
        return false;
    }

    public static boolean isShadowPass()
    {
        return false;
    }

    public static boolean beginBufferUpload(BufferBuilder builder)
    {
        return false;
    }

    public static void endBufferUpload(boolean extended)
    {}

    public static float[] calculateTangents(float[] v, float[] n, float[] u)
    {
        return v;
    }

    public static float[] calculateTangents(float[] t, float[] v, float[] n, float[] u)
    {
        return t;
    }

    public static void addUniforms(List<CachedUniform> list, Map<String, ShaderCurves.ShaderVariable> variableMap)
    {}
}
