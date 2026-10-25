package com.bbsvfx.bbsvfx.camera;

import mchorse.bbs_mod.camera.clips.modifiers.EntityClip;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.List;

/**
 * Orbit path: the camera circles a focus point and always looks at it. Use it like a keyframe camera,
 * but the keyframes are the orbit itself: angle around the focus (degrees, can go past 360 for several
 * turns), distance from the focus, pitch (camera elevation), height (lifts the focus), roll and FOV.
 *
 * <p>The focus is the {@code offset} point (a world position), or, when an actor is picked with the
 * inherited {@code selector}, that actor's position plus the offset. Every value is hybrid like the
 * Impact clip: a slider gives a constant, and a keyframed channel overrides it. With no keyframes the
 * camera simply makes {@link #turns} full revolutions over the clip.</p>
 */
public class OrbitPathClip extends EntityClip
{
    public final ValueFloat startAngle = new ValueFloat("startAngle", 0F, -3600F, 3600F);
    /** Full revolutions over the clip when the angle channel has no keyframes (negative = the other way). */
    public final ValueFloat turns = new ValueFloat("turns", 1F, -20F, 20F);
    public final ValueFloat distance = new ValueFloat("distance", 5F, 0F, 500F);
    public final ValueFloat pitch = new ValueFloat("pitch", 15F, -89F, 89F);
    public final ValueFloat height = new ValueFloat("height", 0F, -500F, 500F);
    public final ValueFloat roll = new ValueFloat("roll", 0F, -180F, 180F);
    public final ValueFloat fov = new ValueFloat("fov", 70F, 1F, 170F);

    public final KeyframeChannel<Double> angleKf = new KeyframeChannel<>("angle_kf", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> distanceKf = new KeyframeChannel<>("distance_kf", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> pitchKf = new KeyframeChannel<>("pitch_kf", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> heightKf = new KeyframeChannel<>("height_kf", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> rollKf = new KeyframeChannel<>("roll_kf", KeyframeFactories.DOUBLE);
    public final KeyframeChannel<Double> fovKf = new KeyframeChannel<>("fov_kf", KeyframeFactories.DOUBLE);

    public OrbitPathClip()
    {
        super();

        this.add(this.startAngle);
        this.add(this.turns);
        this.add(this.distance);
        this.add(this.pitch);
        this.add(this.height);
        this.add(this.roll);
        this.add(this.fov);
        this.add(this.angleKf);
        this.add(this.distanceKf);
        this.add(this.pitchKf);
        this.add(this.heightKf);
        this.add(this.rollKf);
        this.add(this.fovKf);
    }

    private static float read(KeyframeChannel<Double> channel, ValueFloat slider, float t)
    {
        return channel.isEmpty() ? slider.get() : channel.interpolate(t).floatValue();
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float t = context.relativeTick + context.transition;
        int length = Math.max(1, this.duration.get());

        float angle = this.angleKf.isEmpty()
            ? this.startAngle.get() + this.turns.get() * 360F * MathUtils.clamp(t / length, 0F, 1F)
            : this.angleKf.interpolate(t).floatValue();
        float distance = Math.max(0F, read(this.distanceKf, this.distance, t));
        float pitch = MathUtils.clamp(read(this.pitchKf, this.pitch, t), -89F, 89F);
        float height = read(this.heightKf, this.height, t);
        float roll = read(this.rollKf, this.roll, t);
        float fov = read(this.fovKf, this.fov, t);

        /* Focus point: an actor's position + offset, or just the offset as a world position. */
        Point offset = this.offset.get();
        double fx = offset.x;
        double fy = offset.y + height;
        double fz = offset.z;
        List<IEntity> entities = this.getEntities(context);

        if (!entities.isEmpty())
        {
            IEntity entity = entities.get(0);

            fx += Lerps.lerp(entity.getPrevX(), entity.getX(), context.transition);
            fy += Lerps.lerp(entity.getPrevY(), entity.getY(), context.transition);
            fz += Lerps.lerp(entity.getPrevZ(), entity.getZ(), context.transition);
        }

        double a = Math.toRadians(angle);
        double p = Math.toRadians(pitch);
        double cx = fx + Math.sin(a) * Math.cos(p) * distance;
        double cy = fy + Math.sin(p) * distance;
        double cz = fz + Math.cos(a) * Math.cos(p) * distance;

        Angle look = Angle.angle(fx - cx, fy - cy, fz - cz);

        position.point.set(cx, cy, cz);
        position.angle.set(look.yaw, look.pitch, roll, fov);
    }

    @Override
    protected Clip create()
    {
        return new OrbitPathClip();
    }
}
