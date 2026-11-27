package mchorse.bbs_mod.utils;

/** CSS-style cubic bezier easing through (0,0), (x1,y1), (x2,y2), (1,1). */
public class BezierEase
{
    public static final float[][] PRESETS = {
        {0.42F, 0F, 0.58F, 1F},
        {0F, 0F, 1F, 1F},
        {0.42F, 0F, 1F, 1F},
        {0F, 0F, 0.58F, 1F},
        {0.65F, 0F, 0.35F, 1F},
        {0.16F, 1F, 0.3F, 1F}
    };
    public static final String[] LABELS = {"Ease in-out", "Linear", "Ease in", "Ease out", "Smooth (strong)", "Snap out"};

    public static float ease(float t, float x1, float y1, float x2, float y2)
    {
        t = Math.max(0F, Math.min(1F, t));

        if (t == 0F || t == 1F)
        {
            return t;
        }

        /* Solve x(s) = t by bisection, then read y(s) */
        float lo = 0F;
        float hi = 1F;
        float s = t;

        for (int i = 0; i < 24; i++)
        {
            s = (lo + hi) / 2F;

            if (bezier(s, x1, x2) < t)
            {
                lo = s;
            }
            else
            {
                hi = s;
            }
        }

        return bezier(s, y1, y2);
    }

    private static float bezier(float s, float a, float b)
    {
        float u = 1F - s;

        return 3F * u * u * s * a + 3F * u * s * s * b + s * s * s;
    }
}
