package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.api.client.editor.FilmEditorTool;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.utils.RayTracing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * Draw a path: while on, every left click in the viewport drops a position keyframe for the
 * selected replay on the block under the cursor, then moves the cursor forward by the step.
 * The motion path is shown while drawing. Esc or right click ends it.
 */
public class DrawPathTool extends FilmEditorTool
{
    private static boolean active;

    private final UIFilmController controller;

    public DrawPathTool(UIFilmController controller)
    {
        this.controller = controller;
    }

    public static boolean isActive()
    {
        return active;
    }

    public static void toggle()
    {
        active = !active;

        if (active)
        {
            BBSSettings.editorMotionPath.enabled.set(true);
        }
    }

    @Override
    public boolean click(UIContext context)
    {
        if (!active)
        {
            return false;
        }

        if (context.mouseButton == 1)
        {
            active = false;

            return true;
        }

        if (context.mouseButton != 0)
        {
            return false;
        }

        Area area = this.controller.panel.preview.getViewport();
        World world = MinecraftClient.getInstance().world;

        if (world == null || this.controller.panel.replayEditor.getReplay() == null || !area.isInside(context))
        {
            return false;
        }

        Camera camera = this.controller.panel.getCamera();
        Vector3f rayOffset = new Vector3f();
        Vector3f rayDirection = camera.getMouseRay(context.mouseX, context.mouseY, area.x, area.y, area.w, area.h, rayOffset);

        HitResult result = RayTracing.rayTrace(
            world,
            RayTracing.fromVector3d(new Vector3d(camera.position).add(rayOffset.x, rayOffset.y, rayOffset.z)),
            RayTracing.fromVector3f(rayDirection),
            512F
        );

        if (result.getType() == HitResult.Type.BLOCK)
        {
            this.controller.panel.replayEditor.moveReplay(result.getPos().x, result.getPos().y, result.getPos().z);
            this.controller.panel.setCursor(this.controller.panel.getCursor() + BBSSettings.drawPathStep.get());
        }

        return true;
    }

    @Override
    public boolean key(UIContext context)
    {
        if (active && context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            active = false;

            return true;
        }

        return false;
    }
}
