package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.utils.BezierEase;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** Crane shot: the camera rises or descends, swings sideways, tilts and zooms over the clip. */
public class CraneClip extends CameraClip
{
    public final ValueFloat height = new ValueFloat("height", 4F, -500F, 500F);
    public final ValueFloat swing = new ValueFloat("swing", 0F, -500F, 500F);
    public final ValueFloat tilt = new ValueFloat("tilt", 0F, -90F, 90F);
    public final ValueFloat zoom = new ValueFloat("zoom", 0F);
    public final ValueFloat easeX1 = new ValueFloat("easeX1", 0.42F, 0F, 1F);
    public final ValueFloat easeY1 = new ValueFloat("easeY1", 0F, -1F, 2F);
    public final ValueFloat easeX2 = new ValueFloat("easeX2", 0.58F, 0F, 1F);
    public final ValueFloat easeY2 = new ValueFloat("easeY2", 1F, -1F, 2F);

    public CraneClip()
    {
        super();

        this.add(this.height);
        this.add(this.swing);
        this.add(this.tilt);
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

        /* Sideways is the camera's own right, flattened to the ground: yaw 0 looks +Z, its right is -X */
        double yaw = Math.toRadians(position.angle.yaw);
        double right = this.swing.get() * t;

        position.point.x += -Math.cos(yaw) * right;
        position.point.z += -Math.sin(yaw) * right;
        position.point.y += this.height.get() * t;
        position.angle.pitch = MathUtils.clamp(position.angle.pitch + this.tilt.get() * t, -90F, 90F);
        position.angle.fov += this.zoom.get() * t;
    }

    @Override
    protected Clip create()
    {
        return new CraneClip();
    }
}
