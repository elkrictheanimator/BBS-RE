package mchorse.bbs_mod.camera.clips.misc;

/**
 * A screen-wide effect handed from a clip to the frame overlay renderer: letterbox bars
 * (ratio > 0) or a flat colour tint (ratio == 0) with the given alpha.
 */
public class FrameBars
{
    /** Target aspect ratio of the picture between the bars; 0 for a full-screen tint. */
    public float ratio;
    /** ARGB colour; the alpha is the strength. */
    public int color;
    /** 0..1: how far the bars have closed in. */
    public float factor = 1F;
    /** 0: bars / tint, 1: eyelids closing from top and bottom, 2: vignette (dark edges), 3: scanlines, 4: frame border, 5: glitch bars. */
    public int kind;
    /** 0..1: softness of the eyelid edge, or how far a vignette reaches into the frame. */
    public float soft;

    public int seed;

    public FrameBars seed(int seed)
    {
        this.seed = seed;

        return this;
    }

    public FrameBars kind(int kind, float soft)
    {
        this.kind = kind;
        this.soft = soft;

        return this;
    }

    public FrameBars(float ratio, int color, float factor)
    {
        this.ratio = ratio;
        this.color = color;
        this.factor = factor;
    }
}
