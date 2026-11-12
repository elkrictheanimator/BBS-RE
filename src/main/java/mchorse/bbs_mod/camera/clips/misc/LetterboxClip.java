package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.ArrayList;
import java.util.List;

/** Cinematic black bars: crops the frame to a wider (or taller) aspect ratio; the envelope slides them in and out. */
public class LetterboxClip extends CameraClip
{
    public static final String[] LABELS = {"2.39:1 (Scope)", "2.35:1 (Cinema)", "2.00:1 (Univisium)", "1.85:1 (Flat)", "16:9 (HD)", "4:3 (Academy)", "1:1 (Square)", "9:16 (Shorts)", "Custom"};
    public static final float[] RATIOS = {2.39F, 2.35F, 2F, 1.85F, 16F / 9F, 4F / 3F, 1F, 9F / 16F, -1F};

    public final ValueInt preset = new ValueInt("preset", 0, 0, RATIOS.length - 1);
    public final ValueFloat customRatio = new ValueFloat("customRatio", 2.39F, 0.2F, 10F);
    public final ValueInt color = new ValueInt("color", Colors.A100);

    public LetterboxClip()
    {
        super();

        this.add(this.preset);
        this.add(this.customRatio);
        this.add(this.color);
    }

    public static List<FrameBars> getBars(ClipContext context)
    {
        return context.clipData.get("frameBars", ArrayList::new);
    }

    public float getRatio()
    {
        float ratio = RATIOS[this.preset.get()];

        return ratio <= 0 ? this.customRatio.get() : ratio;
    }

    @Override
    public void apply(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        float factor = this.envelope.factorEnabled(this.duration.get(), context.relativeTick + context.transition);

        getBars(context).add(new FrameBars(this.getRatio(), this.color.get() | Colors.A100, factor));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new LetterboxClip();
    }
}
