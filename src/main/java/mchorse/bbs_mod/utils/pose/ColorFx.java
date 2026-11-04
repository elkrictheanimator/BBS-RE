package mchorse.bbs_mod.utils.pose;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.AutoBezier;
import mchorse.bbs_mod.utils.interps.IInterp;
import mchorse.bbs_mod.utils.interps.Lerps;

/**
 * A colour effect for a bone: a gradient along an axis that blends the bone toward a second colour
 * source (a plain colour, a rainbow, a pulsing neon or an animated image). Strength 0 = off.
 *
 * <p>Every field is plain data so the effect can be copied, compared, blended between keyframes and
 * saved with the pose it belongs to.</p>
 */
public class ColorFx
{
    public static final int STYLE_SOLID = 0;
    public static final int STYLE_RAINBOW = 1;
    public static final int STYLE_NEON = 2;
    public static final int STYLE_IMAGE = 3;

    /** 0 = off, 1 = the effect fully replaces the bone colour at the far end. */
    public float strength;
    public final Color color2 = new Color().set(Colors.WHITE);
    /** 0 = X, 1 = Y, 2 = Z. */
    public int axis = 1;
    /** Model units the colour takes to fade in. Negative flips the direction. */
    public float length = 16F;
    /** Slides where the gradient starts along the axis, in model units. */
    public float offset;
    /** 0 = tints like Color (multiplies the texture), 1 = washes like Color overlay. */
    public int mode;
    /** 0 = measured from the bone's pivot, 1 = from the model's origin. */
    public int space;
    public int style;
    /** Animation speed in cycles per second (rainbow scroll, neon pulse, image scroll). */
    public float speed;
    /** Rainbow: how many full hue cycles fit in one gradient length. */
    public float cycles = 1F;
    /** Image style: texture link, as text; empty = none. */
    public String image = "";
    /** Image style: frames stacked top to bottom in the picture, and how fast they play. */
    public int frames = 1;
    public float fps = 12F;
    public float tiling = 1F;
    /** Brightness gradient: ramps along the same axis/offset/length, from the lit look to full bright (1) and past it. 0 = off. */
    public float glow;

    public boolean isOff()
    {
        return this.strength <= 0F && this.glow <= 0F;
    }

    public void reset()
    {
        this.strength = 0F;
        this.color2.set(Colors.WHITE);
        this.axis = 1;
        this.length = 16F;
        this.offset = 0F;
        this.mode = 0;
        this.space = 0;
        this.style = STYLE_SOLID;
        this.speed = 0F;
        this.cycles = 1F;
        this.image = "";
        this.frames = 1;
        this.fps = 12F;
        this.tiling = 1F;
        this.glow = 0F;
    }

    public void copy(ColorFx fx)
    {
        this.strength = fx.strength;
        this.color2.copy(fx.color2);
        this.axis = fx.axis;
        this.length = fx.length;
        this.offset = fx.offset;
        this.mode = fx.mode;
        this.space = fx.space;
        this.style = fx.style;
        this.speed = fx.speed;
        this.cycles = fx.cycles;
        this.image = fx.image;
        this.frames = fx.frames;
        this.fps = fx.fps;
        this.tiling = fx.tiling;
        this.glow = fx.glow;
    }

    /** Plain blend toward another effect (a pose crossfade): numbers mix, choices switch at the end. */
    public void lerp(ColorFx fx, float a)
    {
        if (a >= 1F)
        {
            this.axis = fx.axis;
            this.mode = fx.mode;
            this.space = fx.space;
            this.style = fx.style;
            this.image = fx.image;
            this.frames = fx.frames;
        }

        this.strength = Lerps.lerp(this.strength, fx.strength, a);
        this.color2.r = Lerps.lerp(this.color2.r, fx.color2.r, a);
        this.color2.g = Lerps.lerp(this.color2.g, fx.color2.g, a);
        this.color2.b = Lerps.lerp(this.color2.b, fx.color2.b, a);
        this.length = Lerps.lerp(this.length, fx.length, a);
        this.offset = Lerps.lerp(this.offset, fx.offset, a);
        this.speed = Lerps.lerp(this.speed, fx.speed, a);
        this.cycles = Lerps.lerp(this.cycles, fx.cycles, a);
        this.fps = Lerps.lerp(this.fps, fx.fps, a);
        this.tiling = Lerps.lerp(this.tiling, fx.tiling, a);
        this.glow = Lerps.lerp(this.glow, fx.glow, a);
    }

    /** Keyframe interpolation (the curve of the segment). */
    public void interpolate(ColorFx preA, ColorFx a, ColorFx b, ColorFx postB, IInterp interp, float x)
    {
        this.choices(a, b, x);

        this.strength = (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA.strength, a.strength, b.strength, postB.strength, x)), 0F, 1F);
        this.color2.set(
            (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA.color2.r, a.color2.r, b.color2.r, postB.color2.r, x)), 0F, 1F),
            (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA.color2.g, a.color2.g, b.color2.g, postB.color2.g, x)), 0F, 1F),
            (float) MathUtils.clamp(interp.interpolate(IInterp.context.set(preA.color2.b, a.color2.b, b.color2.b, postB.color2.b, x)), 0F, 1F),
            1F
        );
        this.length = (float) interp.interpolate(IInterp.context.set(preA.length, a.length, b.length, postB.length, x));
        this.offset = (float) interp.interpolate(IInterp.context.set(preA.offset, a.offset, b.offset, postB.offset, x));
        this.speed = (float) interp.interpolate(IInterp.context.set(preA.speed, a.speed, b.speed, postB.speed, x));
        this.cycles = (float) interp.interpolate(IInterp.context.set(preA.cycles, a.cycles, b.cycles, postB.cycles, x));
        this.fps = (float) interp.interpolate(IInterp.context.set(preA.fps, a.fps, b.fps, postB.fps, x));
        this.tiling = (float) interp.interpolate(IInterp.context.set(preA.tiling, a.tiling, b.tiling, postB.tiling, x));
        this.glow = (float) Math.max(0D, interp.interpolate(IInterp.context.set(preA.glow, a.glow, b.glow, postB.glow, x)));
    }

    public void autoInterpolate(ColorFx preA, ColorFx a, ColorFx b, ColorFx postB, float pt, float at, float bt, float qt, boolean clamped, float x)
    {
        this.choices(a, b, x);

        this.strength = (float) MathUtils.clamp(AutoBezier.get(preA.strength, a.strength, b.strength, postB.strength, pt, at, bt, qt, clamped, x), 0F, 1F);
        this.color2.set(
            (float) MathUtils.clamp(AutoBezier.get(preA.color2.r, a.color2.r, b.color2.r, postB.color2.r, pt, at, bt, qt, clamped, x), 0F, 1F),
            (float) MathUtils.clamp(AutoBezier.get(preA.color2.g, a.color2.g, b.color2.g, postB.color2.g, pt, at, bt, qt, clamped, x), 0F, 1F),
            (float) MathUtils.clamp(AutoBezier.get(preA.color2.b, a.color2.b, b.color2.b, postB.color2.b, pt, at, bt, qt, clamped, x), 0F, 1F),
            1F
        );
        this.length = (float) AutoBezier.get(preA.length, a.length, b.length, postB.length, pt, at, bt, qt, clamped, x);
        this.offset = (float) AutoBezier.get(preA.offset, a.offset, b.offset, postB.offset, pt, at, bt, qt, clamped, x);
        this.speed = (float) AutoBezier.get(preA.speed, a.speed, b.speed, postB.speed, pt, at, bt, qt, clamped, x);
        this.cycles = (float) AutoBezier.get(preA.cycles, a.cycles, b.cycles, postB.cycles, pt, at, bt, qt, clamped, x);
        this.fps = (float) AutoBezier.get(preA.fps, a.fps, b.fps, postB.fps, pt, at, bt, qt, clamped, x);
        this.tiling = (float) AutoBezier.get(preA.tiling, a.tiling, b.tiling, postB.tiling, pt, at, bt, qt, clamped, x);
        this.glow = (float) Math.max(0D, AutoBezier.get(preA.glow, a.glow, b.glow, postB.glow, pt, at, bt, qt, clamped, x));
    }

    private void choices(ColorFx a, ColorFx b, float x)
    {
        ColorFx pick = x < 1F ? a : b;

        this.axis = pick.axis;
        this.mode = pick.mode;
        this.space = pick.space;
        this.style = pick.style;
        this.image = pick.image;
        this.frames = pick.frames;
    }

    /** Stacking two layers of the same bone: the later one wins while it is on. */
    public void add(ColorFx fx)
    {
        if (!fx.isOff())
        {
            this.copy(fx);
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof ColorFx fx))
        {
            return false;
        }

        return this.strength == fx.strength && this.color2.equals(fx.color2) && this.axis == fx.axis
            && this.length == fx.length && this.offset == fx.offset && this.mode == fx.mode && this.space == fx.space
            && this.style == fx.style && this.speed == fx.speed && this.cycles == fx.cycles
            && this.image.equals(fx.image) && this.frames == fx.frames && this.fps == fx.fps && this.tiling == fx.tiling && this.glow == fx.glow;
    }

    @Override
    public int hashCode()
    {
        int hash = Float.floatToIntBits(this.strength);

        hash = 31 * hash + this.color2.getARGBColor();
        hash = 31 * hash + this.axis;
        hash = 31 * hash + Float.floatToIntBits(this.length);
        hash = 31 * hash + Float.floatToIntBits(this.offset);
        hash = 31 * hash + this.mode;
        hash = 31 * hash + this.space;
        hash = 31 * hash + this.style;
        hash = 31 * hash + Float.floatToIntBits(this.speed);
        hash = 31 * hash + Float.floatToIntBits(this.cycles);
        hash = 31 * hash + this.image.hashCode();
        hash = 31 * hash + this.frames;
        hash = 31 * hash + Float.floatToIntBits(this.fps);
        hash = 31 * hash + Float.floatToIntBits(this.tiling);
        hash = 31 * hash + Float.floatToIntBits(this.glow);

        return hash;
    }

    /** Written only when the effect is on, so poses without it stay exactly as they were. */
    public void toData(MapType data)
    {
        if (this.isOff())
        {
            return;
        }

        MapType map = new MapType();

        map.putFloat("strength", this.strength);
        map.putInt("color2", this.color2.getARGBColor());
        map.putInt("axis", this.axis);
        map.putFloat("length", this.length);
        map.putFloat("offset", this.offset);
        map.putInt("mode", this.mode);
        map.putInt("space", this.space);
        map.putInt("style", this.style);
        map.putFloat("speed", this.speed);
        map.putFloat("cycles", this.cycles);
        map.putString("image", this.image);
        map.putInt("frames", this.frames);
        map.putFloat("fps", this.fps);
        map.putFloat("tiling", this.tiling);
        map.putFloat("glow", this.glow);

        data.put("color_fx", map);
    }

    public void fromData(MapType data)
    {
        this.reset();

        if (!data.has("color_fx"))
        {
            return;
        }

        MapType map = data.getMap("color_fx");

        this.strength = map.getFloat("strength");
        this.color2.set(map.getInt("color2", Colors.WHITE));
        this.axis = map.getInt("axis", 1);
        this.length = map.has("length") ? map.getFloat("length") : 16F;
        this.offset = map.getFloat("offset");
        this.mode = map.getInt("mode", 0);
        this.space = map.getInt("space", 0);
        this.style = map.getInt("style", 0);
        this.speed = map.getFloat("speed");
        this.cycles = map.has("cycles") ? map.getFloat("cycles") : 1F;
        this.image = map.getString("image", "");
        this.frames = Math.max(1, map.getInt("frames", 1));
        this.fps = map.has("fps") ? map.getFloat("fps") : 12F;
        this.tiling = map.has("tiling") ? map.getFloat("tiling") : 1F;
        this.glow = map.getFloat("glow");
    }
}
