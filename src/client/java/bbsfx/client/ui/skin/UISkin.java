package bbsfx.client.ui.skin;

import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.RoundedBoxes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/** Minimal stand-in for the toolkit skin: just the helpers the ported sliders draw with. */
@Environment(EnvType.CLIENT)
public final class UISkin
{
    public static int accent = 0xFF7B68EE;
    public static boolean glass = false;
    public static int glassAlpha = 200;
    public static int rounding = 3;

    private UISkin()
    {}

    public static void ensureLoaded()
    {
        accent = mchorse.bbs_mod.BBSSettings.primaryColor.get() | 0xFF000000;
    }

    public static void roundedBox(Batcher2D batcher, float x1, float y1, float x2, float y2, int radius, int color)
    {
        RoundedBoxes.render(batcher, x1, y1, x2, y2, radius, color);
    }

    public static void roundedOutline(Batcher2D batcher, float x1, float y1, float x2, float y2, int radius, int color)
    {
        batcher.outline(x1, y1, x2, y2, color);
    }
}
