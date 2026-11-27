package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

/** Darkened (or coloured) screen edges; the clip's envelope fades it in and out. */
public class VignetteClip extends CameraClip
{
    public final ValueFloat strength = new ValueFloat("strength", 0.7F, 0F, 1F);
    public final ValueFloat size = new ValueFloat("size", 0.35F, 0.05F, 0.6F);
    public final ValueInt color = new ValueInt("color", Colors.A100);
    /** Heartbeat: how many times the vignette pulses over the clip (0 = steady). */
    public final ValueInt pulses = new ValueInt("pulses", 0, 0, 40);

    public VignetteClip()
    {
        super();

        this.add(this.strength);
        this.add(this.size);
        this.add(this.color);
        this.add(this.pulses);
    }

    @Override
    public void apply(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        float factor = this.envelope.factorEnabled(this.duration.get(), context.relativeTick + context.transition);
        int pulses = this.pulses.get();

        if (pulses > 0)
        {
            float t = Math.min(1F, Math.max(0F, (context.relativeTick + context.transition) / Math.max(1, this.duration.get())));
            float beat = (float) Math.sin(Math.PI * t * pulses);

            factor *= 0.4F + 0.6F * beat * beat;
        }

        int color = Colors.setA(this.color.get() & Colors.RGB, this.strength.get() * factor);

        LetterboxClip.getBars(context).add(new FrameBars(0F, color, 1F).kind(2, this.size.get()));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new VignetteClip();
    }
}
