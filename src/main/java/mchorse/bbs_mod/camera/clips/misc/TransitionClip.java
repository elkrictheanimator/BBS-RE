package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

/** A full-screen colour transition over the clip's length: fade out, fade in, dip, or flash. */
public class TransitionClip extends CameraClip
{
    public static final String[] LABELS = {"Fade out (to colour)", "Fade in (from colour)", "Dip to colour", "Flash"};

    public final ValueInt type = new ValueInt("type", 0, 0, LABELS.length - 1);
    public final ValueInt color = new ValueInt("color", Colors.A100);
    public final ValueFloat intensity = new ValueFloat("intensity", 1F, 0F, 1F);

    public TransitionClip()
    {
        super();

        this.add(this.type);
        this.add(this.color);
        this.add(this.intensity);
    }

    @Override
    public void apply(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        int duration = Math.max(1, this.duration.get());
        float t = Math.min(1F, Math.max(0F, (context.relativeTick + context.transition) / duration));
        float alpha;

        switch (this.type.get())
        {
            case 0 -> alpha = t;
            case 1 -> alpha = 1F - t;
            case 2 -> alpha = 1F - Math.abs(2F * t - 1F);
            default -> alpha = (1F - t) * (1F - t);
        }

        alpha *= this.intensity.get();

        int color = Colors.setA(this.color.get() & Colors.RGB, alpha);

        LetterboxClip.getBars(context).add(new FrameBars(0F, color, 1F));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new TransitionClip();
    }
}
