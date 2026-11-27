package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

/** Eyelids: blink, wake up (open from closed), fall asleep (slowly close), squint (half closed). */
public class BlinkClip extends CameraClip
{
    public static final String[] LABELS = {"Blink", "Wake up (open)", "Fall asleep (close)", "Squint (hold)"};

    public final ValueInt mode = new ValueInt("mode", 0, 0, LABELS.length - 1);
    public final ValueInt blinks = new ValueInt("blinks", 1, 1, 8);
    public final ValueFloat closed = new ValueFloat("closed", 1F, 0F, 1F);
    public final ValueFloat soft = new ValueFloat("soft", 0.25F, 0F, 1F);
    public final ValueInt color = new ValueInt("color", Colors.A100);

    public BlinkClip()
    {
        super();

        this.add(this.mode);
        this.add(this.blinks);
        this.add(this.closed);
        this.add(this.soft);
        this.add(this.color);
    }

    private static float smooth(float x)
    {
        x = Math.min(1F, Math.max(0F, x));

        return x * x * (3F - 2F * x);
    }

    /** How closed the eyes are (0..1) at normalized time t. */
    public float closure(float t)
    {
        switch (this.mode.get())
        {
            case 1: return 1F - smooth(t);
            case 2: return smooth(t);
            case 3: return smooth(Math.min(1F, t * 4F)) * (1F - smooth(Math.max(0F, t * 4F - 3F)));
            default:
            {
                float n = t * this.blinks.get();
                float local = n - (float) Math.floor(n);

                if (t >= 1F)
                {
                    local = 1F;
                }

                /* quick close (35%), short hold, slower open */
                if (local < 0.35F) return smooth(local / 0.35F);
                if (local < 0.5F) return 1F;

                return 1F - smooth((local - 0.5F) / 0.5F);
            }
        }
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
        float factor = this.closure(t) * this.closed.get();

        if (factor <= 0F)
        {
            return;
        }

        LetterboxClip.getBars(context).add(new FrameBars(0F, this.color.get() | Colors.A100, factor).kind(1, this.soft.get()));
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new BlinkClip();
    }
}
