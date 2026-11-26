package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.modifiers.EntityClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

/**
 * First Person: rides a replay's own point of view (its eyes, or behind/in front of it) and, unlike
 * the plain tracker, keeps the game's first-person hand and HUD on screen so the take looks like
 * what the player saw. The camera itself is placed by the client class.
 */
public class FirstPersonClip extends EntityClip
{
    public static final int MODE_FIRST = 0;
    public static final int MODE_THIRD_BACK = 1;
    public static final int MODE_THIRD_FRONT = 2;

    /** 0 = through the eyes, 1 = behind the replay, 2 = in front of it. */
    public final ValueInt mode = new ValueInt("mode", MODE_FIRST, 0, 2);
    public final ValueFloat fov = new ValueFloat("fov", 70F);
    public final ValueBoolean showHand = new ValueBoolean("show_hand", true);
    public final ValueBoolean showHud = new ValueBoolean("show_hud", true);

    public FirstPersonClip()
    {
        super();

        this.add(this.mode);
        this.add(this.fov);
        this.add(this.showHand);
        this.add(this.showHud);
    }

    @Override
    public void apply(ClipContext context, Position position)
    {
        super.apply(context, position);

        if (this.enabled.get() && (this.showHand.get() || this.showHud.get()))
        {
            boolean[] flags = context.clipData.get("handHud", () -> new boolean[2]);

            flags[0] |= this.showHand.get();
            flags[1] |= this.showHud.get();
        }
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {}

    @Override
    protected Clip create()
    {
        return new FirstPersonClip();
    }
}
