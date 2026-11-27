package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/**
 * Procedural camera shake: layered (fractal) noise driven by a frequency, an intensity and an
 * amplitude for every axis, with ready-made characters (handheld, running, explosion...).
 *
 * <p>It is a pure function of the tick and the seed, so scrubbing, playback and rendering agree.
 * Fade it in or out with the clip's envelope — an explosion is just a strong shake that fades.</p>
 */
public class ProceduralShakeClip extends CameraClip
{
    /** Name, frequency (Hz), octaves, position amplitude xyz (blocks), rotation amplitude pitch/yaw/roll (degrees). */
    public enum Preset
    {
        HANDHELD("Handheld", 1.2F, 3, 0.02F, 0.02F, 0.01F, 0.8F, 0.9F, 0.6F),
        WALKING("Walking", 2.2F, 2, 0.03F, 0.08F, 0.02F, 1.4F, 0.8F, 1F),
        RUNNING("Running", 3.6F, 2, 0.07F, 0.18F, 0.05F, 2.8F, 1.6F, 2.2F),
        EXPLOSION("Explosion", 8.5F, 4, 0.35F, 0.35F, 0.35F, 4.5F, 5.5F, 6.5F),
        IMPACT("Impact", 6F, 3, 0.1F, 0.22F, 0.15F, 3.5F, 2F, 3F),
        FOOTSTEP("Footstep", 1.8F, 2, 0.02F, 0.05F, 0.01F, 0.9F, 0.4F, 0.5F),
        VEHICLE("Vehicle", 12F, 3, 0.02F, 0.04F, 0.02F, 0.6F, 0.5F, 0.9F),
        CUSTOM("Custom", 1.5F, 3, 0.05F, 0.05F, 0.05F, 1F, 1F, 1F),
        /* Added after Custom so the saved preset indexes of older clips keep their meaning */
        EARTHQUAKE("Earthquake", 9F, 4, 0.12F, 0.1F, 0.12F, 2.2F, 2.2F, 3.5F),
        HELICOPTER("Helicopter", 14F, 2, 0.02F, 0.05F, 0.02F, 0.5F, 0.4F, 1F),
        BOAT("Boat / waves", 0.5F, 2, 0.04F, 0.15F, 0.03F, 1.8F, 0.6F, 3F),
        RECOIL("Gun recoil", 7F, 2, 0.03F, 0.06F, 0.12F, 4F, 1.2F, 1.5F),
        HEARTBEAT("Heartbeat", 1.6F, 1, 0F, 0.02F, 0F, 0.5F, 0F, 0.3F),
        DRUNK("Drunk / dizzy", 0.6F, 3, 0.08F, 0.08F, 0.05F, 3F, 5F, 6F),
        ENGINE("Engine idle", 18F, 2, 0.01F, 0.015F, 0.01F, 0.25F, 0.25F, 0.35F),
        UNDERWATER("Underwater sway", 0.35F, 2, 0.05F, 0.1F, 0.05F, 1.2F, 1.5F, 2F);

        public final String label;
        public final float frequency;
        public final int octaves;
        public final float posX, posY, posZ, rotPitch, rotYaw, rotRoll;

        Preset(String label, float frequency, int octaves, float posX, float posY, float posZ, float rotPitch, float rotYaw, float rotRoll)
        {
            this.label = label;
            this.frequency = frequency;
            this.octaves = octaves;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.rotPitch = rotPitch;
            this.rotYaw = rotYaw;
            this.rotRoll = rotRoll;
        }
    }

    public final ValueInt preset = new ValueInt("preset", Preset.HANDHELD.ordinal(), 0, Preset.values().length - 1);
    public final ValueFloat frequency = new ValueFloat("frequency", Preset.HANDHELD.frequency, 0.01F, 100F);
    public final ValueInt octaves = new ValueInt("octaves", Preset.HANDHELD.octaves, 1, 6);
    public final ValueInt seed = new ValueInt("seed", 0);
    public final ValueFloat intensity = new ValueFloat("intensity", 1F, 0F, 100F);
    public final ValueFloat posX = new ValueFloat("posX", Preset.HANDHELD.posX);
    public final ValueFloat posY = new ValueFloat("posY", Preset.HANDHELD.posY);
    public final ValueFloat posZ = new ValueFloat("posZ", Preset.HANDHELD.posZ);
    public final ValueFloat rotPitch = new ValueFloat("rotPitch", Preset.HANDHELD.rotPitch);
    public final ValueFloat rotYaw = new ValueFloat("rotYaw", Preset.HANDHELD.rotYaw);
    public final ValueFloat rotRoll = new ValueFloat("rotRoll", Preset.HANDHELD.rotRoll);
    public final ValueFloat rotFov = new ValueFloat("rotFov", 0F);

    public ProceduralShakeClip()
    {
        super();

        this.add(this.preset);
        this.add(this.frequency);
        this.add(this.octaves);
        this.add(this.seed);
        this.add(this.intensity);
        this.add(this.posX);
        this.add(this.posY);
        this.add(this.posZ);
        this.add(this.rotPitch);
        this.add(this.rotYaw);
        this.add(this.rotRoll);
        this.add(this.rotFov);
    }

    public void applyPreset(Preset preset)
    {
        this.preset.set(preset.ordinal());

        if (preset == Preset.CUSTOM)
        {
            return;
        }

        this.frequency.set(preset.frequency);
        this.octaves.set(preset.octaves);
        this.posX.set(preset.posX);
        this.posY.set(preset.posY);
        this.posZ.set(preset.posZ);
        this.rotPitch.set(preset.rotPitch);
        this.rotYaw.set(preset.rotYaw);
        this.rotRoll.set(preset.rotRoll);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float time = (context.ticks + context.transition) / 20F * this.frequency.get();
        float k = this.intensity.get();
        int seed = this.seed.get();
        int octaves = this.octaves.get();

        position.point.x += fbm(time, seed * 7 + 0, octaves) * this.posX.get() * k;
        position.point.y += fbm(time, seed * 7 + 1, octaves) * this.posY.get() * k;
        position.point.z += fbm(time, seed * 7 + 2, octaves) * this.posZ.get() * k;
        position.angle.pitch += fbm(time, seed * 7 + 3, octaves) * this.rotPitch.get() * k;
        position.angle.yaw += fbm(time, seed * 7 + 4, octaves) * this.rotYaw.get() * k;
        position.angle.roll += fbm(time, seed * 7 + 5, octaves) * this.rotRoll.get() * k;
        position.angle.fov += fbm(time, seed * 7 + 6, octaves) * this.rotFov.get() * k;
    }

    /** Fractal noise in roughly [-1, 1]: every octave doubles the frequency and halves the weight. */
    private static float fbm(float x, int channel, int octaves)
    {
        float sum = 0F;
        float weight = 1F;
        float total = 0F;

        for (int i = 0; i < octaves; i++)
        {
            sum += noise(x, channel * 31 + i) * weight;
            total += weight;
            x *= 2F;
            weight *= 0.5F;
        }

        return sum / total;
    }

    private static float noise(float x, int channel)
    {
        int cell = (int) Math.floor(x);
        float t = x - cell;

        t = t * t * (3F - 2F * t);

        float a = hash(cell, channel);
        float b = hash(cell + 1, channel);

        return a + (b - a) * t;
    }

    private static float hash(int cell, int channel)
    {
        int h = cell * 374761393 + channel * 668265263;

        h = (h ^ (h >>> 13)) * 1274126177;
        h = h ^ (h >>> 16);

        return (h >>> 8) / (float) (1 << 24) * 2F - 1F;
    }

    @Override
    protected Clip create()
    {
        return new ProceduralShakeClip();
    }
}
