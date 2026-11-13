package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

/** Screen overlays: CRT scanlines, a photo / picture frame, or flickering glitch bars. */
public class ScreenFxClip extends CameraClip
{
    public static final String[] LABELS = {"Scanlines (CRT)", "Frame border", "Glitch bars"};

    public final ValueInt mode = new ValueInt("mode", 0, 0, LABELS.length - 1);
    public final ValueFloat strength = new ValueFloat("strength", 0.35F, 0F, 1F);
    public final ValueFloat size = new ValueFloat("size", 0.4F, 0.02F, 1F);
    public final ValueInt color = new ValueInt("color", Colors.A100);
    public final ValueInt seed = new ValueInt("seed", 0, 0, 9999);

    public ScreenFxClip()
    {
        super();

        this.add(this.mode);
        this.add(this.strength);
        this.add(this.size);
        this.add(this.color);
        this.add(this.seed);
    }

    @Override
    public void apply(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        float factor = this.envelope.factorEnabled(this.duration.get(), context.relativeTick + context.transition);
        int color = Colors.setA(this.color.get() & Colors.RGB, this.strength.get() * factor);
        /* glitch bars hop every couple of ticks so they flicker instead of crawling */
        int step = this.seed.get() * 131 + (int) Math.floor((context.relativeTick) / 2F) * 7919;

        LetterboxClip.getBars(context).add(new FrameBars(0F, color, 1F).kind(3 + this.mode.get(), this.size.get()).seed(step));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new ScreenFxClip();
    }
}
