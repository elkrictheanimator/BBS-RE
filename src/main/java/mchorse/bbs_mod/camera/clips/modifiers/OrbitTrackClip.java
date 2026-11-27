package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.BezierEase;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/**
 * Orbit track: the subject is the point one orbit radius straight ahead of the camera (flattened to
 * the horizontal). Over the clip the camera circles that point by the sweep angle, always facing it,
 * optionally rising (a helix) and zooming. Works on whatever the clips below produce.
 */
public class OrbitTrackClip extends CameraClip
{
    public final ValueFloat angle = new ValueFloat("angle", 90F, -3600F, 3600F);
    public final ValueFloat distance = new ValueFloat("distance", 6F, 0.1F, 500F);
    public final ValueFloat height = new ValueFloat("height", 0F);
    public final ValueFloat zoom = new ValueFloat("zoom", 0F);
    public final ValueFloat easeX1 = new ValueFloat("easeX1", 0.42F, 0F, 1F);
    public final ValueFloat easeY1 = new ValueFloat("easeY1", 0F, -1F, 2F);
    public final ValueFloat easeX2 = new ValueFloat("easeX2", 0.58F, 0F, 1F);
    public final ValueFloat easeY2 = new ValueFloat("easeY2", 1F, -1F, 2F);

    public OrbitTrackClip()
    {
        super();

        this.add(this.angle);
        this.add(this.distance);
        this.add(this.height);
        this.add(this.zoom);
        this.add(this.easeX1);
        this.add(this.easeY1);
        this.add(this.easeX2);
        this.add(this.easeY2);
    }

    public void setEase(float[] curve)
    {
        this.easeX1.set(curve[0]);
        this.easeY1.set(curve[1]);
        this.easeX2.set(curve[2]);
        this.easeY2.set(curve[3]);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        float progress = (context.relativeTick + context.transition) / Math.max(1, this.duration.get());
        float t = BezierEase.ease(progress, this.easeX1.get(), this.easeY1.get(), this.easeX2.get(), this.easeY2.get());

        org.joml.Vector3f forward = mchorse.bbs_mod.utils.joml.Matrices.rotation(
            mchorse.bbs_mod.utils.MathUtils.toRad(position.angle.pitch),
            mchorse.bbs_mod.utils.MathUtils.toRad(-position.angle.yaw)).negate();

        forward.y = 0F;

        if (forward.lengthSquared() < 1.0E-6F)
        {
            forward.set(0F, 0F, 1F);
        }

        forward.normalize().mul(this.distance.get());

        /* The subject, and the camera's offset from it */
        double sx = position.point.x + forward.x;
        double sz = position.point.z + forward.z;
        double rx = -forward.x;
        double rz = -forward.z;

        /* Positive angle: the camera swings to its right */
        double a = Math.toRadians(this.angle.get() * t);
        double nx = rx * Math.cos(a) + rz * Math.sin(a);
        double nz = -rx * Math.sin(a) + rz * Math.cos(a);

        position.point.x = sx + nx;
        position.point.z = sz + nz;
        position.point.y += this.height.get() * t;
        position.angle.yaw = Angle.angle(sx - position.point.x, 0, sz - position.point.z).yaw;
        position.angle.fov += this.zoom.get() * t;
    }

    @Override
    protected Clip create()
    {
        return new OrbitTrackClip();
    }
}
