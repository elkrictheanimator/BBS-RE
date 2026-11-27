package mchorse.bbs_mod.utils.iris;

import java.util.Collections;
import java.util.Map;

/** Forge build: no Iris, so there is no shader menu. */
public class IrisShaderMenu
{
    public static Map<String, String> getShadersRawLanguageMap(String language)
    {
        return Collections.emptyMap();
    }

    public static ShaderMenu buildShaderMenu()
    {
        return null;
    }
}
