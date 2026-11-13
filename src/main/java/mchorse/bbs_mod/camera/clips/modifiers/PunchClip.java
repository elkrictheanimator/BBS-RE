package mchorse.bbs_mod.camera.clips.modifiers;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/** Camera effect: punch the FOV / tilt the camera (dutch angle) with a hold, pulse, decay or build shape. */
public class PunchClip extends CameraClip
{
    public static final String[] SHAPES = {"Hold", "Pulse (up and back)", "Decay (hit, then settle)", "Build (ramp up)"};

    public final ValueInt shape = new ValueInt("shape", 1, 0, SHAPES.length - 1);
    public final ValueFloat fov = new ValueFloat("fov", -12F, -120F, 120F);
    public final ValueFloat roll = new ValueFloat("roll", 0F, -180F, 180F);
    public final ValueFloat pitch = new ValueFloat("pitch", 0F, -90F, 90F);
    public final ValueFloat yaw = new ValueFloat("yaw", 0F, -180F, 180F);

    public PunchClip()
    {
        super();

        this.add(this.shape);
        this.add(this.fov);
        this.add(this.roll);
        this.add(this.pitch);
        this.add(this.yaw);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        int duration = Math.max(1, this.duration.get());
        float t = Math.min(1F, Math.max(0F, (context.relativeTick + context.transition) / duration));
        float k;

        switch (this.shape.get())
        {
            case 1 -> k = (float) Math.sin(Math.PI * t);
            case 2 -> k = (1F - t) * (1F - t);
            case 3 -> k = t * t * (3F - 2F * t);
            default -> k = 1F;
        }

        position.angle.fov += this.fov.get() * k;
        position.angle.roll += this.roll.get() * k;
        position.angle.pitch += this.pitch.get() * k;
        position.angle.yaw += this.yaw.get() * k;
    }

    @Override
    protected Clip create()
    {
        return new PunchClip();
    }
}
