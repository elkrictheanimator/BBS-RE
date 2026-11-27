package bbsfx.client.lyrics;

import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import bbsfx.text.Lyrics3DClip;
import bbsfx.text.TextFlashClip;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.FilmControllerContext;
import mchorse.bbs_mod.film.FilmEntityRenderer;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.utils.RenderFrame;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import com.mojang.blaze3d.systems.VertexSorter;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public final class ActorLayer {
   private static ActorLayer.Snapshot snapshot;
   private static BaseFilmController active;
   private static long errorEpoch = -1L;
   private static boolean depthMask;

   private ActorLayer() {
   }

   public static void capture(BaseFilmController controller, WorldRenderContext world) {
      if (!BBSRendering.isIrisShadowPass()) {
         active = controller;
         if (Lyrics3DRenderer.cameraContext(controller) != null) {
            long epoch = RenderFrame.getEpoch();
            if (snapshot == null || snapshot.epoch != epoch) {
               MatrixStack stack = new MatrixStack();
               stack.peek().getPositionMatrix().set(world.matrixStack().peek().getPositionMatrix());
               stack.peek().getNormalMatrix().set(world.matrixStack().peek().getNormalMatrix());
               snapshot = new ActorLayer.Snapshot(
                  epoch,
                  controller,
                  world.camera(),
                  stack,
                  world.consumers(),
                  new Matrix4f(world.projectionMatrix()),
                  new Matrix4f(RenderSystem.getModelViewMatrix()),
                  RenderSystem.getVertexSorting(),
                  controller.paused ? 0.0F : world.tickCounter().getTickDelta(false)
               );
            }
         }
      }
   }

   public static void clear(BaseFilmController controller) {
      if (active == controller) {
         active = null;
      }

      if (snapshot != null && snapshot.controller == controller) {
         snapshot = null;
      }

      depthMask = false;
   }

   public static void render3D(BaseFilmController controller, List<Lyrics3DClip.Frame> frames) {
      ActorLayer.Snapshot current = snapshot;
      if (current != null && current.controller == controller) {
         Set<String> actors = new LinkedHashSet();

         for (Lyrics3DClip.Frame frame : frames) {
            add(actors, (String)frame.clip.actor.get());
         }

         draw(current, actors);
      }
   }

   public static void prepareOverlay(MatrixStack stack, Batcher2D batcher, ClipContext context) {
      depthMask = false;
      ActorLayer.Snapshot current = snapshot;
      if (BBSRendering.isIrisShadersEnabled() && current != null && current.controller.film != null && current.consumers != null) {
         if (belongsTo(context, current)) {
            List<TextFlashClip.Frame> flashes = TextFlashClip.getFrames(context);
            if (!flashes.isEmpty()) {
               Set<String> actors = new LinkedHashSet();

               for (Lyrics3DClip.Frame frame : Lyrics3DClip.getFrames(context)) {
                  add(actors, (String)frame.clip.actor.get());
               }

               for (TextFlashClip.Frame frame : flashes) {
                  add(actors, (String)frame.clip.actor.get());
               }

               if (!actors.isEmpty()) {
                  if (maskDepth(current, actors)) {
                     depthMask = true;
                  }
               }
            }
         }
      }
   }

   public static boolean depthMaskActive() {
      return depthMask;
   }

   public static void renderOverlay(MatrixStack stack, Batcher2D batcher, ClipContext context) {
      depthMask = false;
      if (overlayComposites(BBSRendering.isIrisShadersEnabled())) {
         ActorLayer.Snapshot current = snapshot;
         if (current != null && current.controller.film != null && belongsTo(context, current)) {
            Set<String> actors = new LinkedHashSet();

            for (Lyrics3DClip.Frame frame : Lyrics3DClip.getFrames(context)) {
               add(actors, (String)frame.clip.actor.get());
            }

            for (TextFlashClip.Frame frame : TextFlashClip.getFrames(context)) {
               add(actors, (String)frame.clip.actor.get());
            }

            draw(current, actors);
         }
      }
   }

   private static boolean belongsTo(ClipContext context, ActorLayer.Snapshot current) {
      return !(context instanceof CameraClipContext camera && camera.clips != current.controller.film.camera);
   }

   private static void add(Set<String> actors, String id) {
      if (id != null && !id.isEmpty()) {
         actors.add(id);
      }
   }

   public static boolean compositeWorldActors(boolean iris) {
      return true;
   }

   public static boolean overlayComposites(boolean iris) {
      return !iris;
   }

   private static void draw(ActorLayer.Snapshot current, Set<String> actors) {
      if (!actors.isEmpty() && current.consumers != null && current.controller.film != null) {
         ActorLayer.GlState state = new ActorLayer.GlState();

         try {
            RenderSystem.setProjectionMatrix(current.projection, current.sorting);
            RenderSystem.getModelViewMatrix().set(current.modelView);
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
            RenderSystem.depthMask(true);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            drawActors(current, actors);
            flush(current);
         } catch (RuntimeException e) {
            report(e);
         } finally {
            state.restore();
         }
      }
   }

   private static boolean maskDepth(ActorLayer.Snapshot current, Set<String> actors) {
      ActorLayer.GlState state = new ActorLayer.GlState();
      float[] clearDepth = new float[1];
      boolean ok = true;
      GL11.glGetFloatv(2931, clearDepth);

      try {
         RenderSystem.disableScissor();
         RenderSystem.colorMask(false, false, false, false);
         RenderSystem.disableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.depthFunc(515);
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setProjectionMatrix(current.projection, current.sorting);
         RenderSystem.getModelViewMatrix().set(current.modelView);
         RenderSystem.clearDepth(1.0);
         RenderSystem.clear(256, false);
         RenderSystem.clearDepth(clearDepth[0]);
         drawActors(current, actors);
         flush(current);
      } catch (RuntimeException e) {
         report(e);
         ok = false;
      } finally {
         RenderSystem.clearDepth(clearDepth[0]);
         state.restore();
      }

      return ok;
   }

   private static void drawActors(ActorLayer.Snapshot current, Set<String> actors) {
      for (String id : actors) {
         Replay replay = current.controller.film.replays.getById(id);
         if (replay != null && (Boolean)replay.enabled.get()) {
            IEntity entity = (IEntity)current.controller.entities.get(id);
            if (entity != null && entity.getForm() != null) {
               FilmControllerContext.instance
                  .setup(current.controller.entities, entity, replay, current.camera, current.stack, current.consumers, current.transition)
                  .shadow(false, 0.0F)
                  .nameTag("")
                  .relative((Boolean)replay.relative.get());
               FilmEntityRenderer.renderEntity(FilmControllerContext.instance);
            }
         }
      }
   }

   private static void flush(ActorLayer.Snapshot current) {
      if (current.consumers instanceof Immediate immediate) {
         immediate.draw();
      }
   }

   private static void report(RuntimeException e) {
      long epoch = RenderFrame.getEpoch();
      if (epoch != errorEpoch) {
         errorEpoch = epoch;
         System.err.println("[bbsfx] Actor layer skipped this frame: " + e);
      }
   }

   @Environment(EnvType.CLIENT)
   private static final class GlState {
      private final Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
      private final Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
      private final VertexSorter sorting = RenderSystem.getVertexSorting();
      private final boolean depth = GL11.glIsEnabled(2929);
      private final int depthFunc = GL11.glGetInteger(2932);
      private final boolean depthMask = GL11.glGetBoolean(2930);
      private final boolean blend = GL11.glIsEnabled(3042);
      private final int blendSrcRgb = GL11.glGetInteger(32969);
      private final int blendDstRgb = GL11.glGetInteger(32968);
      private final int blendSrcAlpha = GL11.glGetInteger(32971);
      private final int blendDstAlpha = GL11.glGetInteger(32970);
      private final boolean cull = GL11.glIsEnabled(2884);
      private final float[] shaderColor = (float[])RenderSystem.getShaderColor().clone();
      private final boolean[] colorMask = colorMask();
      private final boolean scissor = GL11.glIsEnabled(3089);
      private final int[] scissorBox = scissorBox();

      private void restore() {
         RenderSystem.setProjectionMatrix(this.projection, this.sorting);
         RenderSystem.getModelViewMatrix().set(this.modelView);
         RenderSystem.depthFunc(this.depthFunc);
         RenderSystem.depthMask(this.depthMask);
         RenderSystem.colorMask(this.colorMask[0], this.colorMask[1], this.colorMask[2], this.colorMask[3]);
         GlStateManager._blendFuncSeparate(this.blendSrcRgb, this.blendDstRgb, this.blendSrcAlpha, this.blendDstAlpha);
         RenderSystem.setShaderColor(this.shaderColor[0], this.shaderColor[1], this.shaderColor[2], this.shaderColor[3]);
         if (this.depth) {
            RenderSystem.enableDepthTest();
         } else {
            RenderSystem.disableDepthTest();
         }

         if (this.blend) {
            RenderSystem.enableBlend();
         } else {
            RenderSystem.disableBlend();
         }

         if (this.cull) {
            RenderSystem.enableCull();
         } else {
            RenderSystem.disableCull();
         }

         if (this.scissor) {
            RenderSystem.enableScissor(this.scissorBox[0], this.scissorBox[1], this.scissorBox[2], this.scissorBox[3]);
         } else {
            RenderSystem.disableScissor();
         }
      }

      private static boolean[] colorMask() {
         ByteBuffer buffer = ByteBuffer.allocateDirect(4);
         GL11.glGetBooleanv(3107, buffer);
         return new boolean[]{buffer.get(0) != 0, buffer.get(1) != 0, buffer.get(2) != 0, buffer.get(3) != 0};
      }

      private static int[] scissorBox() {
         int[] box = new int[4];
         GL11.glGetIntegerv(3088, box);
         return box;
      }
   }

   @Environment(EnvType.CLIENT)
   private record Snapshot(
      long epoch,
      BaseFilmController controller,
      Camera camera,
      MatrixStack stack,
      VertexConsumerProvider consumers,
      Matrix4f projection,
      Matrix4f modelView,
      VertexSorter sorting,
      float transition
   ) {
   }
}
