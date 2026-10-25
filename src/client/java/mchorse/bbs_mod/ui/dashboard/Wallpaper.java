package mchorse.bbs_mod.ui.dashboard;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.video.LoopVideo;

/**
 * The video wallpaper: one looping video, played on the shared clock, drawn wherever a screen
 * asks for it. Every caller shares one decoder, so the landing screen and the editors behind it
 * never decode the same file twice.
 */
public class Wallpaper
{
    /** How opaque the panels stay over the video: enough to read, little enough to see it */
    public static final float SURFACE_OPACITY = 0.72F;

    /** The veil over the video, so panels and text stay readable on a busy picture */
    private static final int VEIL = 0x66000000;

    /** The wallpaper's frame right now, or null when none is set or it hasn't decoded yet. */
    public static Texture frame()
    {
        return LoopVideo.frame(BBSSettings.backgroundVideo.get(), BBSSettings.backgroundVideoUrl.get());
    }

    /**
     * Draw the wallpaper over the rectangle, covering all of it and cropping what overflows.
     * Returns false when there is nothing to draw (no video, or no frame yet), so the caller
     * can fall back to what it showed before.
     */
    public static boolean draw(Batcher2D batcher, int x, int y, int w, int h)
    {
        Texture texture = frame();

        if (texture == null || texture.width <= 0 || texture.height <= 0 || w <= 0 || h <= 0)
        {
            return false;
        }

        float areaAspect = (float) w / h;
        float texAspect = (float) texture.width / texture.height;
        float cropW = texture.width;
        float cropH = texture.height;

        if (areaAspect > texAspect)
        {
            cropH = texture.width / areaAspect;
        }
        else
        {
            cropW = texture.height * areaAspect;
        }

        float u1 = (texture.width - cropW) * 0.5F;
        float v1 = (texture.height - cropH) * 0.5F;

        batcher.texturedBox(texture, Colors.WHITE, x, y, w, h, u1, v1, u1 + cropW, v1 + cropH, texture.width, texture.height);
        batcher.box(x, y, x + w, y + h, VEIL);

        return true;
    }
}
