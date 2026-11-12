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

    public FrameBars(float ratio, int color, float factor)
    {
        this.ratio = ratio;
        this.color = color;
        this.factor = factor;
    }
}
