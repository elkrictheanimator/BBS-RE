package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;

/**
 * One clip for every screen / camera effect: eyelids (blink, wake up, fall asleep, squint), screen overlays
 * (vignette, scanlines, frame, glitch), camera punch, and the Minecraft status effects (nausea, blindness,
 * darkness, pumpkin, powder snow, hurt, nether portal, underwater, fire, poison, wither, low health).
 */
public class CameraEffectClip extends CameraClip
{
    public static final String[] LABELS = {
        "Blink", "Wake up (open eyes)", "Fall asleep (close eyes)", "Squint (hold)",
        "Vignette", "Scanlines (CRT)", "Frame border", "Glitch bars", "Punch / dutch tilt",
        "MC: Nausea", "MC: Blindness", "MC: Darkness", "MC: Pumpkin head", "MC: Powder snow (freezing)",
        "MC: Hurt (damage)", "MC: Nether portal", "MC: Underwater", "MC: On fire", "MC: Poison", "MC: Wither", "MC: Low health"
    };

    public static final String[] SHAPES = {"Hold", "Pulse (up and back)", "Decay (hit, then settle)", "Build (ramp up)"};

    public final ValueInt type = new ValueInt("type", 0, 0, LABELS.length - 1);
    public final ValueFloat strength = new ValueFloat("strength", 0.8F, 0F, 1F);
    public final ValueFloat size = new ValueFloat("size", 0.35F, 0.02F, 1F);
    public final ValueFloat speed = new ValueFloat("speed", 1F, 0.1F, 6F);
    public final ValueBoolean customColor = new ValueBoolean("customColor", false);
    public final ValueInt color = new ValueInt("color", Colors.A100);
    public final ValueInt seed = new ValueInt("seed", 0, 0, 9999);
    public final ValueInt pulses = new ValueInt("pulses", 0, 0, 40);

    /* eyelids */
    public final ValueInt blinks = new ValueInt("blinks", 1, 1, 8);
    public final ValueFloat closed = new ValueFloat("closed", 1F, 0F, 1F);
    public final ValueFloat soft = new ValueFloat("soft", 0.3F, 0F, 1F);
    public final ValueFloat curve = new ValueFloat("curve", 0.6F, 0F, 1.5F);
    public final ValueFloat dip = new ValueFloat("dip", 3F, -30F, 30F);

    /* punch */
    public final ValueInt shape = new ValueInt("shape", 1, 0, SHAPES.length - 1);
    public final ValueFloat fov = new ValueFloat("fov", -12F, -120F, 120F);
    public final ValueFloat roll = new ValueFloat("roll", 0F, -180F, 180F);
    public final ValueFloat pitch = new ValueFloat("pitch", 0F, -90F, 90F);
    public final ValueFloat yaw = new ValueFloat("yaw", 0F, -180F, 180F);

    public CameraEffectClip()
    {
        super();

        this.add(this.type);
        this.add(this.strength);
        this.add(this.size);
        this.add(this.speed);
        this.add(this.customColor);
        this.add(this.color);
        this.add(this.seed);
        this.add(this.pulses);
        this.add(this.blinks);
        this.add(this.closed);
        this.add(this.soft);
        this.add(this.curve);
        this.add(this.dip);
        this.add(this.shape);
        this.add(this.fov);
        this.add(this.roll);
        this.add(this.pitch);
        this.add(this.yaw);
    }

    private static float smooth(float x)
    {
        x = Math.min(1F, Math.max(0F, x));

        return x * x * (3F - 2F * x);
    }

    /** How closed the eyes are (0..1) at normalized time t, for the eyelid types 0..3. */
    public float closure(float t)
    {
        switch (this.type.get())
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

                /* quick close (30%), short hold, slower and heavier open */
                if (local < 0.3F) return smooth(local / 0.3F);
                if (local < 0.42F) return 1F;

                return 1F - smooth((local - 0.42F) / 0.58F);
            }
        }
    }

    private int col(int fallback)
    {
        return this.customColor.get() ? (this.color.get() & Colors.RGB) : (fallback & Colors.RGB);
    }

    private static int argb(int rgb, float alpha)
    {
        return Colors.setA(rgb & Colors.RGB, Math.min(1F, Math.max(0F, alpha)));
    }

    private static void tint(java.util.List<FrameBars> bars, int rgb, float alpha)
    {
        if (alpha > 0.002F)
        {
            bars.add(new FrameBars(0F, argb(rgb, alpha), 1F));
        }
    }

    private static void vignette(java.util.List<FrameBars> bars, int rgb, float alpha, float size)
    {
        if (alpha > 0.002F)
        {
            bars.add(new FrameBars(0F, argb(rgb, alpha), 1F).kind(2, size));
        }
    }

    private static float wave(float s, float freq, float phase)
    {
        return (float) Math.sin(s * freq + phase);
    }

    @Override
    public void apply(ClipContext context, Position position)
    {
        if (!this.enabled.get())
        {
            return;
        }

        int duration = Math.max(1, this.duration.get());
        float ticks = context.relativeTick + context.transition;
        float t = Math.min(1F, Math.max(0F, ticks / duration));
        float env = this.envelope.factorEnabled(duration, ticks);
        float a = this.strength.get() * env;
        float s = ticks / 20F * this.speed.get();
        int type = this.type.get();
        java.util.List<FrameBars> bars = LetterboxClip.getBars(context);

        if (type <= 3)
        {
            float closure = this.closure(t);
            float factor = closure * this.closed.get();

            if (factor > 0F)
            {
                bars.add(new FrameBars(0F, this.col(0) | Colors.A100, factor).kind(6, this.soft.get()).curve(this.curve.get()));
                /* the peripheral vision darkens a bit before the lids meet */
                vignette(bars, this.col(0), 0.55F * closure, 0.5F);
                position.angle.pitch += this.dip.get() * closure;
            }

            return;
        }

        float k;

        switch (type)
        {
            case 4:
            {
                float f = 1F;

                if (this.pulses.get() > 0)
                {
                    float beat = (float) Math.sin(Math.PI * t * this.pulses.get());

                    f = 0.4F + 0.6F * beat * beat;
                }

                vignette(bars, this.col(0), a * f, this.size.get());

                break;
            }
            case 5:
            case 6:
            case 7:
            {
                int step = this.seed.get() * 131 + (int) Math.floor(context.relativeTick / 2F) * 7919;

                bars.add(new FrameBars(0F, argb(this.col(0), a), 1F).kind(type - 2, this.size.get()).seed(step));

                break;
            }
            case 8:
            {
                switch (this.shape.get())
                {
                    case 1 -> k = (float) Math.sin(Math.PI * t);
                    case 2 -> k = (1F - t) * (1F - t);
                    case 3 -> k = t * t * (3F - 2F * t);
                    default -> k = 1F;
                }

                k *= env;
                position.angle.fov += this.fov.get() * k;
                position.angle.roll += this.roll.get() * k;
                position.angle.pitch += this.pitch.get() * k;
                position.angle.yaw += this.yaw.get() * k;

                break;
            }
            case 9: /* nausea */
            {
                position.angle.roll += wave(s, 2.1F, 0F) * 5F * a;
                position.angle.pitch += wave(s, 1.7F, 1F) * 2.5F * a;
                position.angle.yaw += wave(s, 1.3F, 2F) * 3F * a;
                position.angle.fov += wave(s, 0.9F, 0.5F) * 5F * a;
                tint(bars, this.col(0x6BD36B), a * (0.16F + 0.06F * wave(s, 1.5F, 0F)));
                vignette(bars, this.col(0x2E7D32), a * 0.35F, 0.4F);

                break;
            }
            case 10: /* blindness */
            {
                vignette(bars, this.col(0), a, 0.6F);
                tint(bars, this.col(0), a * 0.4F);

                break;
            }
            case 11: /* darkness */
            {
                float p = 0.5F + 0.5F * wave(s, 1.5F, 0F);

                tint(bars, this.col(0), a * (0.22F + 0.3F * p));
                vignette(bars, this.col(0), a * 0.85F, 0.5F);

                break;
            }
            case 12: /* pumpkin */
            {
                vignette(bars, this.col(0), a, 0.55F);
                vignette(bars, this.col(0), a, 0.38F);
                vignette(bars, this.col(0), a * 0.8F, 0.2F);

                break;
            }
            case 13: /* powder snow */
            {
                vignette(bars, this.col(0xBFE8FF), a * 0.85F, 0.45F);
                tint(bars, this.col(0x9ED8FF), a * 0.12F);
                position.angle.roll += wave(s, 38F, 0F) * 0.45F * a;
                position.angle.yaw += wave(s, 31F, 1F) * 0.3F * a;

                break;
            }
            case 14: /* hurt */
            {
                k = (1F - t) * (1F - t) * env * this.strength.get();

                tint(bars, this.col(0xFF1A1A), k * 0.5F);
                vignette(bars, this.col(0xB00000), k * 0.9F, 0.4F);
                position.angle.roll += 5F * k * (this.seed.get() % 2 == 0 ? 1F : -1F);

                break;
            }
            case 15: /* nether portal */
            {
                tint(bars, this.col(0x6A1FB8), a * (0.25F + 0.14F * wave(s, 2F, 0F)));
                vignette(bars, this.col(0x3B0A73), a * 0.55F, 0.4F);
                position.angle.roll += wave(s, 1.2F, 0F) * 2.2F * a;
                position.angle.fov += wave(s, 0.8F, 1F) * 3.5F * a;
                position.angle.yaw += wave(s, 1.9F, 2F) * 1.2F * a;

                break;
            }
            case 16: /* underwater */
            {
                tint(bars, this.col(0x2A6FD6), a * 0.28F);
                vignette(bars, this.col(0x0A2A55), a * 0.5F, 0.35F);
                position.angle.roll += wave(s, 0.8F, 0F) * 0.9F * a;
                position.angle.fov += wave(s, 0.5F, 1F) * 1.6F * a;

                break;
            }
            case 17: /* on fire */
            {
                float flick = 0.6F + 0.2F * wave(s, 9F, 0F) + 0.2F * wave(s, 17F, 1F);

                vignette(bars, this.col(0xFF7A18), a * flick, 0.3F);
                tint(bars, this.col(0xFF5500), a * 0.08F * flick);

                break;
            }
            case 18: /* poison */
            {
                float p = 0.5F + 0.5F * wave(s, 2.4F, 0F);

                vignette(bars, this.col(0x4E9331), a * (0.5F + 0.4F * p), 0.4F);
                tint(bars, this.col(0x4E9331), a * 0.1F);

                break;
            }
            case 19: /* wither */
            {
                float p = 0.5F + 0.5F * wave(s, 1.6F, 0F);

                tint(bars, this.col(0x1A1A1A), a * (0.2F + 0.15F * p));
                vignette(bars, this.col(0x111111), a * 0.75F, 0.42F);

                break;
            }
            default: /* low health heartbeat */
            {
                float w = 6.2832F * 1.1F * s;
                float b1 = (float) Math.pow(Math.max(0D, Math.sin(w)), 6);
                float b2 = (float) Math.pow(Math.max(0D, Math.sin(w - 0.9F)), 6);
                float b = Math.min(1F, b1 + 0.6F * b2);

                vignette(bars, this.col(0xC00000), a * (0.3F + 0.7F * b), 0.4F);

                break;
            }
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new CameraEffectClip();
    }
}
