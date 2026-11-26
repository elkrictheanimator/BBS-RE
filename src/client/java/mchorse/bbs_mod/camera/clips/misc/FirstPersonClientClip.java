package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.ui.film.controller.ActorCamera;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import org.joml.Vector3f;

import java.util.List;

public class FirstPersonClientClip extends FirstPersonClip
{
    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        List<IEntity> entities = this.getEntities(context);

        if (entities.isEmpty())
        {
            return;
        }

        IEntity actor = entities.get(0);
        Camera camera = new Camera();
        int mode = this.mode.get();

        if (mode == MODE_FIRST)
        {
            ActorCamera.firstPerson(camera, actor, context.transition);
        }
        else
        {
            ActorCamera.thirdPerson(camera, actor, context.transition, mode == MODE_THIRD_BACK);
        }

        /* Offset is in the camera's own frame: x to the right, y up, z forward */
        Point offset = this.offset.get();

        if (offset.x != 0 || offset.y != 0 || offset.z != 0)
        {
            Vector3f forward = camera.getLookDirection();
            Vector3f right = new Vector3f(forward).cross(0F, 1F, 0F);

            if (right.lengthSquared() > 1.0E-8F)
            {
                right.normalize();
            }

            camera.position.x += right.x * offset.x + forward.x * offset.z;
            camera.position.y += offset.y + forward.y * offset.z;
            camera.position.z += right.z * offset.x + forward.z * offset.z;
        }

        position.set(camera);
        position.angle.fov = this.fov.get();
    }

    @Override
    protected Clip create()
    {
        return new FirstPersonClientClip();
    }
}
