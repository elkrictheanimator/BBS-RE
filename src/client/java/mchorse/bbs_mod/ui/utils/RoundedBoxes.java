package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;

/** Rounded rectangles out of plain boxes: a body and one thin row per pixel of every corner. */
public class RoundedBoxes
{
    public static void render(Batcher2D batcher, float x1, float y1, float x2, float y2, int radius, int color)
    {
        radius = Math.min(radius, (int) Math.min((x2 - x1) / 2F, (y2 - y1) / 2F));

        if (radius <= 0)
        {
            batcher.box(x1, y1, x2, y2, color);

            return;
        }

        batcher.box(x1, y1 + radius, x2, y2 - radius, color);

        for (int i = 0; i < radius; i++)
        {
            float dy = radius - i - 0.5F;
            float inset = radius - (float) Math.sqrt(radius * radius - dy * dy);

            inset = Math.round(inset);

            batcher.box(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            batcher.box(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    /** Fill an area: rounded by the corner radius setting, a plain box when it is 0. */
    public static void fill(Batcher2D batcher, mchorse.bbs_mod.ui.utils.Area area, int color)
    {
        render(batcher, area.x, area.y, area.ex(), area.ey(), radius(), color);
    }

    public static int radius()
    {
        return BBSSettings.uiCornerRadius.get();
    }
}
