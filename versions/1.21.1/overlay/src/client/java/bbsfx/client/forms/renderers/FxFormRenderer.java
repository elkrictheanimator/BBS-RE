package bbsfx.client.forms.renderers;

import bbsfx.forms.FxForm;
import bbsfx.forms.FxParams;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public class FxFormRenderer extends FormRenderer<FxForm> implements ITickable {
   private long ticks;

   public FxFormRenderer(FxForm form) {
      super(form);
   }

   public void tick(IEntity entity) {
      this.ticks++;
   }

   private float time(FormRenderingContext context) {
      if (context.modelRenderer) {
         return (float)context.modelRendererTick + context.getTransition();
      }

      Float filmTime = FilmClock.time(context.entity, context.getTransition());
      return filmTime == null ? (float)this.ticks + context.getTransition() : filmTime;
   }

   public void render3D(FormRenderingContext context) {
      if (!BBSRendering.isIrisShadowPass()) {
         FxFormRenderer.GlState state = new FxFormRenderer.GlState();
         context.stack.push();

         try {
            this.draw(context);
         } finally {
            state.restore();
            context.stack.pop();
         }
      }
   }

   private void draw(FormRenderingContext context) {
      if ((Boolean)((FxForm)this.form).billboard.get()) {
         Matrix4f modelMatrix = context.stack.peek().getPositionMatrix();
         Vector3f scale = Vectors.TEMP_3F;
         modelMatrix.getScale(scale);
         modelMatrix.m00(1.0F).m01(0.0F).m02(0.0F);
         modelMatrix.m10(0.0F).m11(1.0F).m12(0.0F);
         modelMatrix.m20(0.0F).m21(0.0F).m22(1.0F);
         modelMatrix.scale(scale);
         context.stack.peek().getNormalMatrix().identity();
      }

      float t = this.time(context);
      float p = ((FxForm)this.form).loop.get() ? t * 0.02F * (Float)((FxForm)this.form).speed.get() % 1.0F : (Float)((FxForm)this.form).progress.get();
      BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = context.stack.peek().getPositionMatrix();
      Color primary = (Color)((FxForm)this.form).primary.get();
      Color secondary = (Color)((FxForm)this.form).secondary.get();
      float ci = (Float)((FxForm)this.form).intensity.get() * (context.color >>> 24) / 255.0F;
      primary = new Color(primary.r, primary.g, primary.b, primary.a * ci);
      secondary = new Color(secondary.r, secondary.g, secondary.b, secondary.a * ci);
      switch (((FxForm)this.form).effect.get()) {
         case 1:
            this.ring(builder, matrix, t, p, primary, secondary);
            break;
         case 2:
            this.sparks(builder, matrix, t, p, primary, secondary);
            break;
         case 3:
            this.rays(builder, matrix, t, p, primary, secondary);
            break;
         case 4:
            this.orb(builder, matrix, t, p, primary, secondary);
            break;
         case 5:
            this.field(builder, context.stack, t, p, primary, secondary);
            break;
         case 6:
            this.trail(builder, matrix, t, p, primary, secondary);
            break;
         case 7:
            this.lightning(builder, matrix, t, p, primary, secondary);
            break;
         case 8:
            this.spiral(builder, matrix, t, p, primary, secondary);
            break;
         case 9:
            this.stars(builder, matrix, t, p, primary, secondary);
            break;
         case 10:
            this.vortex(builder, matrix, t, p, primary, secondary);
            break;
         case 11:
            this.grid(builder, matrix, t, p, primary, secondary);
            break;
         default:
            this.shockwave(builder, matrix, t, p, primary, secondary);
      }

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      RenderSystem.disableCull();
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if ((Boolean)((FxForm)this.form).additive.get()) {
         RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      }

      RenderSystem.depthMask(true);
      RenderSystem.setShader(GameRenderer::getPositionColorProgram);
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private void shockwave(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int layers = (Integer)((FxForm)this.form).layers.get();
      int segments = FxParams.count((Integer)((FxForm)this.form).count.get(), 48);
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float fade = FxParams.fade((1.0F - p) * (1.0F - p), (Float)((FxForm)this.form).fade.get())
         * FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float mix = (Float)((FxForm)this.form).colorMix.get();

      for (int l = 0; l < layers; l++) {
         float f = FxParams.layer(layers, l);
         float scale = 1.0F + f * 0.18F;
         Matrix4f shifted = new Matrix4f(m)
            .translate(0.0F, FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size), 0.0F)
            .rotateZ(spin + f * 0.35F);
         Color first = blend(a, c, FxParams.mix(mix, f * 0.12F), a.a * fade);
         Color second = blend(a, c, FxParams.mix(mix, 1.0F - f * 0.12F), c.a * fade * 0.8F);
         Color third = blend(a, c, FxParams.mix(mix, f * 0.12F), a.a * fade * 0.6F);
         this.ringXY(b, shifted, size * p * scale, (0.08F * size * (1.0F - p) + 0.02F) * thick, first.r, first.g, first.b, first.a, segments);
         this.ringXY(b, shifted, size * p * 0.82F * scale, (0.05F * size * (1.0F - p) + 0.015F) * thick, second.r, second.g, second.b, second.a, segments);
         this.ringXY(b, shifted, size * p * 0.6F * scale, (0.03F * size * (1.0F - p) + 0.01F) * thick, third.r, third.g, third.b, third.a, segments);
      }
   }

   private static Color blend(Color a, Color c, float mix, float alpha) {
      return new Color(a.r + (c.r - a.r) * mix, a.g + (c.g - a.g) * mix, a.b + (c.b - a.b) * mix, alpha);
   }

   private void vortex(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int layers = (Integer)((FxForm)this.form).layers.get();
      float thick = (Float)((FxForm)this.form).thickness.get() * size;
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float mix0 = (Float)((FxForm)this.form).colorMix.get();
      float fade = (Float)((FxForm)this.form).fade.get();

      for (int l = 0; l < layers; l++) {
         float f = layers == 1 ? 0.5F : (float)l / (layers - 1);
         float radius = size * (0.35F + 0.65F * f) * (1.0F - 0.55F * p);
         float rot = spin + p * 3.2F + f * 1.7F;
         float alpha = a.a * (float)Math.pow(1.0F - f * 0.5F, fade) * (0.35F + 0.65F * (1.0F - p));
         float mix = Math.min(1.0F, mix0 + f * 0.3F);
         float lift = (f - 0.5F) * (Float)((FxForm)this.form).height.get() * size * 0.5F;
         Matrix4f shifted = new Matrix4f(m).translate(0.0F, lift, 0.0F);
         this.arc(b, shifted, radius, thick, rot, a.r + (c.r - a.r) * mix, a.g + (c.g - a.g) * mix, a.b + (c.b - a.b) * mix, alpha, 5);
      }
   }

   private void grid(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int cells = Math.max(2, (Integer)((FxForm)this.form).count.get() / 4);
      float thick = Math.max(0.01F, (Float)((FxForm)this.form).thickness.get() * size * 0.08F);
      float flick = (Float)((FxForm)this.form).flicker.get();
      float mix0 = (Float)((FxForm)this.form).colorMix.get();
      float fade = (Float)((FxForm)this.form).fade.get();
      float pulse = 0.55F + 0.45F * (float)Math.sin(t * 0.004F * (Float)((FxForm)this.form).speed.get() * Math.max(0.05F, flick));

      for (int i = 0; i <= cells; i++) {
         float f = (float)i / cells;
         float pos = (f - 0.5F) * 2.0F * size;
         float alpha = a.a * pulse * (float)Math.pow(Math.max(0.05F, 1.0F - Math.abs(f - 0.5F) * 1.6F), fade);
         this.ribbon(b, m, -size, pos, size, pos, thick, thick, c.r, c.g, c.b, alpha * 0.8F);
         this.ribbon(b, m, pos, -size, pos, size, thick, thick, c.r, c.g, c.b, alpha * 0.8F);
      }

      this.disc(
         b, m, 0.0F, 0.0F, size * 0.35F * (0.6F + 0.4F * p), a.r + (c.r - a.r) * mix0, a.g + (c.g - a.g) * mix0, a.b + (c.b - a.b) * mix0, a.a * 0.35F * pulse
      );
   }

   private void spiral(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int count = (Integer)((FxForm)this.form).count.get();
      float dot = (Float)((FxForm)this.form).thickness.get() * size;
      float turns = 2.4F;
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F + p * (float) (Math.PI * 2);

      for (int i = 0; i < count; i++) {
         float f = (float)i / (count - 1);
         float angle = f * (float) (Math.PI * 2) * turns + spin;
         float radius = size * (0.2F + 0.8F * f) * (0.55F + 0.45F * p);
         float x = (float)Math.cos(angle) * radius;
         float y = (float)Math.sin(angle) * radius;
         float alpha = a.a * (0.25F + 0.75F * f);
         float mix = f;
         this.disc(b, m, x, y, dot * (0.6F + 0.6F * f), a.r + (c.r - a.r) * mix, a.g + (c.g - a.g) * mix, a.b + (c.b - a.b) * mix, alpha);
      }

      this.ringXY(b, m, size * (0.55F + 0.45F * p), 0.02F * size, c.r, c.g, c.b, c.a * 0.5F);
   }

   private void stars(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int count = (Integer)((FxForm)this.form).count.get();
      float dot = (Float)((FxForm)this.form).thickness.get() * size * 0.5F;
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.01F;
      float flicker = (Float)((FxForm)this.form).flicker.get();
      float height = (Float)((FxForm)this.form).height.get();
      float fade = (Float)((FxForm)this.form).fade.get();

      for (int i = 0; i < count; i++) {
         float seed = i * 12.9898F;
         float angle = (float)Math.sin(seed) * 43758.547F;
         float radius = (float)Math.abs(Math.cos(seed * 1.7F)) * 43758.547F;
         float depth = (float)Math.abs(Math.sin(seed * 2.3F)) * 43758.547F;
         angle = (angle - (float)Math.floor(angle)) * (float) (Math.PI * 2) + spin;
         radius = (radius - (float)Math.floor(radius)) * size * (0.35F + 0.65F * p);
         depth = depth - (float)Math.floor(depth) - 0.5F;
         float x = (float)Math.cos(angle) * radius;
         float y = (float)Math.sin(angle) * radius * 0.65F + depth * height * size;
         float twinkle = flicker <= 0.0F ? 0.5F : 0.5F + 0.5F * (float)Math.sin(t * 0.004F * (Float)((FxForm)this.form).speed.get() * flicker + i * 1.7F);
         float alpha = a.a * (0.25F + 0.75F * (float)Math.pow(twinkle, fade));
         float mix = i % 3 / 2.0F;
         this.disc(b, m, x, y, dot * (0.7F + twinkle * 0.8F), a.r + (c.r - a.r) * mix, a.g + (c.g - a.g) * mix, a.b + (c.b - a.b) * mix, alpha);
      }
   }

   private void ring(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      float pulse = 0.75F + 0.25F * (float)Math.sin(t * 0.1F * (Float)((FxForm)this.form).speed.get());
      int layers = (Integer)((FxForm)this.form).layers.get();
      int segments = FxParams.count((Integer)((FxForm)this.form).count.get(), 48);
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float life = FxParams.life(p, (Boolean)((FxForm)this.form).loop.get(), (Float)((FxForm)this.form).fade.get())
         * FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float mix = (Float)((FxForm)this.form).colorMix.get();

      for (int l = 0; l < layers; l++) {
         float f = FxParams.layer(layers, l);
         float scale = 1.0F + f * 0.2F;
         Matrix4f shifted = new Matrix4f(m)
            .translate(0.0F, FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size), 0.0F)
            .rotateZ(spin + f * 0.4F);
         Color first = blend(a, c, FxParams.mix(mix, f * 0.15F), a.a * life);
         Color second = blend(a, c, FxParams.mix(mix, 1.0F - f * 0.15F), c.a * 0.8F * life);
         this.ringXY(b, shifted, size * pulse * scale, 0.06F * size * thick, first.r, first.g, first.b, first.a, segments);
         this.ringXY(b, shifted, size * pulse * 0.7F * scale, 0.03F * size * thick, second.r, second.g, second.b, second.a, segments);
      }
   }

   private void sparks(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int count = FxParams.count((Integer)((FxForm)this.form).count.get(), 28);
      int layers = (Integer)((FxForm)this.form).layers.get();
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float flicker = FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());

      for (int l = 0; l < layers; l++) {
         float layer = FxParams.layer(layers, l);
         Matrix4f shifted = new Matrix4f(m).translate(0.0F, FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size), 0.0F);

         for (int i = 0; i < count; i++) {
            float angle = i * 2.399963F + spin + layer * 0.3F;
            float phase = p * 1.35F - i % 7 * 0.03F - layer * 0.08F;
            float travel = Math.max(0.0F, Math.min(1.05F, phase));
            float fade = FxParams.fade(Math.max(0.0F, 1.0F - travel), (Float)((FxForm)this.form).fade.get());
            float dist = size * travel * (1.0F + layer * 0.12F);
            float droop = -0.25F * size * travel * travel;
            float dx = (float)Math.cos(angle);
            float dy = (float)Math.sin(angle);
            float x = dx * dist;
            float y = dy * dist + droop + hash(i, 171) * (Float)((FxForm)this.form).height.get() * size * 0.3F;
            float len = (0.14F * size * (1.0F - travel) + 0.02F) * thick;
            float tx = -dy * len;
            float ty = dx * len;
            Color color = blend(a, c, FxParams.mix((Float)((FxForm)this.form).colorMix.get(), i % 2 == 0 ? 1.0F : 0.0F), 1.0F);
            float alpha = a.a * fade * flicker;
            this.quad(b, shifted, x - tx, y - ty, x + tx, y + ty, 0.0F, color.r, color.g, color.b, alpha * 0.9F);
         }
      }
   }

   private void rays(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int count = FxParams.count((Integer)((FxForm)this.form).count.get(), 14);
      int layers = (Integer)((FxForm)this.form).layers.get();
      float rotation = t * 0.01F * (Float)((FxForm)this.form).speed.get() + (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float life = FxParams.life(p, (Boolean)((FxForm)this.form).loop.get(), (Float)((FxForm)this.form).fade.get())
         * FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());

      for (int l = 0; l < layers; l++) {
         float layer = FxParams.layer(layers, l);
         float layerScale = 1.0F + layer * 0.15F;
         float height = FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size);

         for (int i = 0; i < count; i++) {
            float angle = rotation + layer * 0.17F + i * (float)((Math.PI * 2) / count);
            float length = size * layerScale * (0.5F + 0.5F * (float)Math.sin(t * 0.07F * (Float)((FxForm)this.form).speed.get() + i * 1.7F));
            float half = (0.09F + 0.05F * (i % 3)) * thick;
            float dx = (float)Math.cos(angle);
            float dy = (float)Math.sin(angle);
            float px = -dy;
            float py = dx;
            float y = height + hash(i, 211) * (Float)((FxForm)this.form).height.get() * size * 0.15F;
            Color color = blend(a, c, FxParams.mix((Float)((FxForm)this.form).colorMix.get(), i % 2 == 0 ? 0.0F : 1.0F), 1.0F);
            b.vertex(m, 0.0F, y, 0.0F).color(color.r, color.g, color.b, a.a * 0.55F * life);
            b.vertex(m, dx * length + px * half, y + dy * length + py * half, 0.0F).color(color.r, color.g, color.b, 0.0F);
            b.vertex(m, dx * length - px * half, y + dy * length - py * half, 0.0F).color(color.r, color.g, color.b, 0.0F);
         }
      }

      Color center = blend(a, c, FxParams.mix((Float)((FxForm)this.form).colorMix.get(), 0.0F), a.a * life);
      this.ringXY(b, m, size * 0.14F, 0.04F * thick, center.r, center.g, center.b, center.a, FxParams.count((Integer)((FxForm)this.form).count.get(), 48));
   }

   private void orb(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      float breath = 1.0F + 0.06F * (float)Math.sin(t * 0.08F * (Float)((FxForm)this.form).speed.get());
      int layers = (Integer)((FxForm)this.form).layers.get();
      int segments = FxParams.count((Integer)((FxForm)this.form).count.get(), 32);
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float life = FxParams.life(p, (Boolean)((FxForm)this.form).loop.get(), (Float)((FxForm)this.form).fade.get())
         * FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float mix = (Float)((FxForm)this.form).colorMix.get();

      for (int l = 0; l < layers; l++) {
         float f = FxParams.layer(layers, l);
         float scale = (1.0F + f * 0.25F) * breath;
         Matrix4f shifted = new Matrix4f(m)
            .translate(0.0F, FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size), 0.0F)
            .rotateZ(spin + f * 0.4F);
         Color outer = blend(a, c, FxParams.mix(mix, f * 0.15F), a.a * 0.25F * life);
         Color middle = blend(a, c, FxParams.mix(mix, 1.0F - f * 0.15F), c.a * 0.4F * life);
         Color core = blend(a, c, FxParams.mix(mix, f * 0.15F), a.a * 0.8F * life);
         Color rim = blend(a, c, FxParams.mix(mix, 1.0F - f * 0.15F), c.a * 0.5F * life);
         this.disc(b, shifted, size * 0.5F * scale, outer.r, outer.g, outer.b, outer.a, segments);
         this.disc(b, shifted, size * 0.32F * scale, middle.r, middle.g, middle.b, middle.a, segments);
         this.disc(b, shifted, size * 0.16F * scale, core.r, core.g, core.b, core.a, segments);
         this.ringXY(b, shifted, size * 0.55F * scale, 0.025F * thick, rim.r, rim.g, rim.b, rim.a, FxParams.count((Integer)((FxForm)this.form).count.get(), 48));
      }
   }

   private void field(BufferBuilder b, MatrixStack stack, float t, float p, Color a, Color c) {
      Matrix4f m = stack.peek().getPositionMatrix();
      float size = (Float)((FxForm)this.form).size.get();
      float spin = t * 0.02F * (Float)((FxForm)this.form).speed.get() + (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      int layers = (Integer)((FxForm)this.form).layers.get();
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float life = FxParams.life(p, (Boolean)((FxForm)this.form).loop.get(), (Float)((FxForm)this.form).fade.get())
         * FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());
      int count = (Integer)((FxForm)this.form).count.get();
      float mix = (Float)((FxForm)this.form).colorMix.get();

      for (int l = 0; l < layers; l++) {
         float f = FxParams.layer(layers, l);
         float scale = 1.0F + f * 0.15F;
         float offset = FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size);
         Color first = blend(a, c, FxParams.mix(mix, f * 0.15F), a.a * life);
         Color second = blend(a, c, FxParams.mix(mix, 1.0F - f * 0.15F), c.a * life);
         stack.push();
         stack.translate(0.0F, offset, 0.0F);
         stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(60.0F));
         this.arc(
            b,
            stack.peek().getPositionMatrix(),
            size * 0.55F * scale,
            0.04F * thick,
            spin + f * 0.3F,
            first.r,
            first.g,
            first.b,
            first.a,
            FxParams.count(count, 8)
         );
         this.arc(
            b,
            stack.peek().getPositionMatrix(),
            size * 0.4F * scale,
            0.03F * thick,
            -spin * 1.4F - f * 0.3F,
            second.r,
            second.g,
            second.b,
            second.a,
            FxParams.count(count, 5)
         );
         stack.pop();
         stack.push();
         stack.translate(0.0F, offset, 0.0F);
         stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(60.0F));
         this.arc(
            b,
            stack.peek().getPositionMatrix(),
            size * 0.5F * scale,
            0.035F * thick,
            -spin * 0.8F,
            second.r,
            second.g,
            second.b,
            second.a * 0.9F,
            FxParams.count(count, 6)
         );
         stack.pop();
         this.disc(b, m, 0.0F, offset, size * 0.1F, first.r, first.g, first.b, first.a, FxParams.count(count, 32));
      }
   }

   private void trail(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      int seg = FxParams.count((Integer)((FxForm)this.form).count.get(), 56);
      int layers = (Integer)((FxForm)this.form).layers.get();
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;
      float flicker = FxParams.flicker((Float)((FxForm)this.form).flicker.get(), t, (Float)((FxForm)this.form).speed.get());
      float mix = (Float)((FxForm)this.form).colorMix.get();

      for (int l = 0; l < layers; l++) {
         float layer = FxParams.layer(layers, l);
         float head = (((FxForm)this.form).loop.get() ? t * 0.04F * (Float)((FxForm)this.form).speed.get() : p * 6.0F) + spin + layer * 0.6F;
         float life = ((FxForm)this.form).loop.get() ? 1.0F : Math.min(1.0F, p * 2.5F);
         float span = 2.6F;
         float radius = size * 0.5F * (1.0F + layer * 0.14F);
         float height = FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size);
         float px = 0.0F;
         float py = 0.0F;
         float pw = 0.0F;

         for (int i = 0; i <= seg; i++) {
            float f = (float)i / seg;
            float angle = head - span * f;
            float x = (float)Math.cos(angle) * radius;
            float y = (float)Math.sin(angle) * radius + height;
            float width = 0.07F * size * (1.0F - f) * thick;
            Color color = blend(a, c, FxParams.mix(mix, f), 1.0F);
            float alpha = FxParams.fade(a.a * life * (1.0F - f) * (1.0F - f), (Float)((FxForm)this.form).fade.get()) * flicker;
            if (i > 0) {
               this.ribbon(b, m, px, py, x, y, pw, width, color.r, color.g, color.b, alpha);
            }

            px = x;
            py = y;
            pw = width;
         }

         Color headColor = blend(a, c, FxParams.mix(mix, layer * 0.2F), 1.0F);
         float alpha = FxParams.fade(a.a * life * 0.85F, (Float)((FxForm)this.form).fade.get()) * flicker;
         this.disc(
            b,
            m,
            (float)Math.cos(head) * radius,
            (float)Math.sin(head) * radius + height,
            0.12F * size * thick,
            headColor.r,
            headColor.g,
            headColor.b,
            alpha,
            FxParams.count((Integer)((FxForm)this.form).count.get(), 32)
         );
      }
   }

   private void lightning(BufferBuilder b, Matrix4f m, float t, float p, Color a, Color c) {
      float size = (Float)((FxForm)this.form).size.get();
      float flickerRate = (Float)((FxForm)this.form).flicker.get();
      float seed = ((FxForm)this.form).loop.get() ? t * 0.35F * (Float)((FxForm)this.form).speed.get() * flickerRate : p * 8.0F;
      int layers = (Integer)((FxForm)this.form).layers.get();
      int steps = FxParams.count((Integer)((FxForm)this.form).count.get(), 20);
      float thick = FxParams.thickness((Float)((FxForm)this.form).thickness.get());
      float spin = (Float)((FxForm)this.form).spin.get() * t * 0.02F;

      for (int l = 0; l < layers; l++) {
         float layer = FxParams.layer(layers, l);
         int shape = (int)Math.floor(seed) + l * 13;
         float appear = ((FxForm)this.form).loop.get() ? 1.0F : Math.min(1.0F, p * 6.0F);
         float flicker = 0.55F + 0.45F * Math.abs(hash(shape, 901));
         float alpha = FxParams.fade(a.a * appear * flicker, (Float)((FxForm)this.form).fade.get())
            * FxParams.flicker(flickerRate, t, (Float)((FxForm)this.form).speed.get());
         float topX = size * 0.22F * hash(shape, 3) + layer * size * 0.15F;
         float topY = size * 1.9F;
         float baseX = size * 0.05F * hash(shape, 4);
         float baseY = 0.0F;
         Matrix4f shifted = new Matrix4f(m)
            .translate(0.0F, FxParams.offset(layers, l, (Float)((FxForm)this.form).height.get(), size), 0.0F)
            .rotateZ(spin + layer * 0.1F);
         Color primary = blend(a, c, FxParams.mix((Float)((FxForm)this.form).colorMix.get(), layer * 0.3F), 1.0F);
         Color secondary = blend(a, c, FxParams.mix((Float)((FxForm)this.form).colorMix.get(), 1.0F - layer * 0.3F), 1.0F);
         this.bolt(b, shifted, topX, topY, baseX, baseY, steps, 0.3F * size, 0.1F * size * thick, secondary, alpha * 0.16F, shape);
         this.bolt(b, shifted, topX, topY, baseX, baseY, steps, 0.3F * size, 0.017F * size * thick, primary, alpha, shape);

         for (int k = 0; k < 3; k++) {
            float f = 0.24F + k * 0.2F;
            float bx = topX + (baseX - topX) * f;
            float by = topY + (baseY - topY) * f;
            float dir = k % 2 == 0 ? 1.0F : -1.0F;
            float ex = bx + dir * size * (0.28F + 0.16F * Math.abs(hash(shape, 30 + k)));
            float ey = by - size * (0.34F + 0.22F * Math.abs(hash(shape, 40 + k)));
            this.bolt(b, shifted, bx, by, ex, ey, 9, 0.11F * size, 0.045F * size * thick, secondary, alpha * 0.12F, shape + 17 + k);
            this.bolt(b, shifted, bx, by, ex, ey, 9, 0.11F * size, 0.008F * size * thick, secondary, alpha * 0.75F, shape + 17 + k);
         }

         for (int k = 0; k < 2; k++) {
            float dir = k == 0 ? 1.0F : -1.0F;
            float ex = baseX + dir * size * (0.26F + 0.1F * Math.abs(hash(shape, 50 + k)));
            float ey = baseY - size * (0.03F + 0.03F * Math.abs(hash(shape, 60 + k)));
            this.bolt(b, shifted, baseX, baseY, ex, ey, 6, 0.05F * size, 0.007F * size * thick, primary, alpha * 0.55F, shape + 71 + k);
         }

         this.disc(
            b,
            shifted,
            baseX,
            baseY,
            size * 0.28F,
            secondary.r,
            secondary.g,
            secondary.b,
            alpha * 0.18F,
            FxParams.count((Integer)((FxForm)this.form).count.get(), 32)
         );
         this.disc(
            b,
            shifted,
            baseX,
            baseY,
            size * 0.14F,
            primary.r,
            primary.g,
            primary.b,
            alpha * 0.55F,
            FxParams.count((Integer)((FxForm)this.form).count.get(), 32)
         );
      }
   }

   private void bolt(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, int steps, float jitter, float width, Color color, float alpha, int seed) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0E-4F)) {
         float nx = -dy / len;
         float ny = dx / len;
         float px = x1;
         float py = y1;
         float pw = width;

         for (int i = 1; i <= steps; i++) {
            float f = (float)i / steps;
            float envelope = (float)Math.sin(Math.PI * f);
            float offset = (hash(seed * 53 + i, 11) * 0.75F + hash(seed * 97 + (i >> 1), 23) * 0.25F) * jitter * envelope;
            float x = x1 + dx * f + nx * offset;
            float y = y1 + dy * f + ny * offset;
            float w = width * (1.0F - 0.85F * f);
            this.ribbon(b, m, px, py, x, y, pw, w, color.r, color.g, color.b, alpha * (1.0F - 0.2F * f));
            px = x;
            py = y;
            pw = w;
         }
      }
   }

   private static float hash(int a, int b) {
      int h = a * 374761393 + b * 668265263;
      h = (h ^ h >>> 13) * 1274126177;
      h ^= h >>> 16;
      return (h >>> 8) / 1.6777216E7F * 2.0F - 1.0F;
   }

   private void ribbon(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, float w1, float w2, float r, float g, float bl, float a) {
      float dx = x2 - x1;
      float dy = y2 - y1;
      float len = (float)Math.sqrt(dx * dx + dy * dy);
      if (!(len < 1.0E-4F)) {
         float nx = -dy / len;
         float ny = dx / len;
         b.vertex(m, x1 + nx * w1, y1 + ny * w1, 0.0F).color(r, g, bl, a);
         b.vertex(m, x2 + nx * w2, y2 + ny * w2, 0.0F).color(r, g, bl, a);
         b.vertex(m, x2 - nx * w2, y2 - ny * w2, 0.0F).color(r, g, bl, a);
         b.vertex(m, x1 + nx * w1, y1 + ny * w1, 0.0F).color(r, g, bl, a);
         b.vertex(m, x2 - nx * w2, y2 - ny * w2, 0.0F).color(r, g, bl, a);
         b.vertex(m, x1 - nx * w1, y1 - ny * w1, 0.0F).color(r, g, bl, a);
      }
   }

   private void ringXY(BufferBuilder b, Matrix4f m, float radius, float thickness, float r, float g, float bl, float a) {
      this.ringXY(b, m, radius, thickness, r, g, bl, a, 48);
   }

   private void ringXY(BufferBuilder b, Matrix4f m, float radius, float thickness, float r, float g, float bl, float a, int seg) {
      float inner = Math.max(0.001F, radius - thickness);
      float outer = radius + thickness;

      for (int i = 0; i < seg; i++) {
         float a0 = (float)((Math.PI * 2) * i / seg);
         float a1 = (float)((Math.PI * 2) * (i + 1) / seg);
         float c0 = (float)Math.cos(a0);
         float s0 = (float)Math.sin(a0);
         float c1 = (float)Math.cos(a1);
         float s1 = (float)Math.sin(a1);
         float alpha = a * (0.75F + 0.25F * c0);
         b.vertex(m, c0 * outer, s0 * outer, 0.0F).color(r, g, bl, alpha);
         b.vertex(m, c1 * outer, s1 * outer, 0.0F).color(r, g, bl, alpha);
         b.vertex(m, c1 * inner, s1 * inner, 0.0F).color(r, g, bl, alpha);
         b.vertex(m, c0 * outer, s0 * outer, 0.0F).color(r, g, bl, alpha);
         b.vertex(m, c1 * inner, s1 * inner, 0.0F).color(r, g, bl, alpha);
         b.vertex(m, c0 * inner, s0 * inner, 0.0F).color(r, g, bl, alpha);
      }
   }

   private void arc(BufferBuilder b, Matrix4f m, float radius, float thickness, float rotation, float r, float g, float bl, float a, int spokes) {
      int steps = Math.max(1, 40 / spokes);

      for (int s = 0; s < spokes; s++) {
         float base = rotation + (float)((Math.PI * 2) * s / spokes);
         float span = (float)((Math.PI * 2) / spokes) * 0.62F;
         float inner = Math.max(0.001F, radius - thickness);
         float outer = radius;

         for (int i = 0; i < steps; i++) {
            float f0 = (float)i / steps;
            float f1 = (float)(i + 1) / steps;
            float a0 = base + span * f0;
            float a1 = base + span * f1;
            float c0 = (float)Math.cos(a0);
            float s0 = (float)Math.sin(a0);
            float c1 = (float)Math.cos(a1);
            float s1 = (float)Math.sin(a1);
            b.vertex(m, c0 * outer, s0 * outer, 0.0F).color(r, g, bl, a);
            b.vertex(m, c1 * outer, s1 * outer, 0.0F).color(r, g, bl, a);
            b.vertex(m, c1 * inner, s1 * inner, 0.0F).color(r, g, bl, a);
            b.vertex(m, c0 * outer, s0 * outer, 0.0F).color(r, g, bl, a);
            b.vertex(m, c1 * inner, s1 * inner, 0.0F).color(r, g, bl, a);
            b.vertex(m, c0 * inner, s0 * inner, 0.0F).color(r, g, bl, a);
         }
      }
   }

   private void disc(BufferBuilder b, Matrix4f m, float radius, float r, float g, float bl, float a) {
      this.disc(b, m, 0.0F, 0.0F, radius, r, g, bl, a);
   }

   private void disc(BufferBuilder b, Matrix4f m, float radius, float r, float g, float bl, float a, int seg) {
      this.disc(b, m, 0.0F, 0.0F, radius, r, g, bl, a, seg);
   }

   private void disc(BufferBuilder b, Matrix4f m, float cx, float cy, float radius, float r, float g, float bl, float a) {
      this.disc(b, m, cx, cy, radius, r, g, bl, a, 32);
   }

   private void disc(BufferBuilder b, Matrix4f m, float cx, float cy, float radius, float r, float g, float bl, float a, int seg) {
      for (int i = 0; i < seg; i++) {
         float a0 = (float)((Math.PI * 2) * i / seg);
         float a1 = (float)((Math.PI * 2) * (i + 1) / seg);
         b.vertex(m, cx, cy, 0.0F).color(r, g, bl, a);
         b.vertex(m, cx + (float)Math.cos(a0) * radius, cy + (float)Math.sin(a0) * radius, 0.0F).color(r, g, bl, 0.0F);
         b.vertex(m, cx + (float)Math.cos(a1) * radius, cy + (float)Math.sin(a1) * radius, 0.0F).color(r, g, bl, 0.0F);
      }
   }

   private void quad(BufferBuilder b, Matrix4f m, float x1, float y1, float x2, float y2, float z, float r, float g, float bl, float a) {
      b.vertex(m, x1, y1, z).color(r, g, bl, a);
      b.vertex(m, x2, y1, z).color(r, g, bl, a);
      b.vertex(m, x2, y2, z).color(r, g, bl, a);
      b.vertex(m, x1, y1, z).color(r, g, bl, a);
      b.vertex(m, x2, y2, z).color(r, g, bl, a);
      b.vertex(m, x1, y2, z).color(r, g, bl, a);
   }

   public void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
      float t = (float)(System.currentTimeMillis() % 100000L) / 50.0F;
      float pulse = 0.75F + 0.25F * (float)Math.sin(t * 0.1F);
      int cx = (x1 + x2) / 2;
      int cy = (y1 + y2) / 2;
      int base = Math.min(x2 - x1, y2 - y1);
      int radius = (int)(base * 0.28F * pulse);
      Color a = (Color)((FxForm)this.form).primary.get();
      Color c = (Color)((FxForm)this.form).secondary.get();
      int argb = ((int)(a.a * 220.0F) & 0xFF) << 24 | ((int)(a.r * 255.0F) & 0xFF) << 16 | ((int)(a.g * 255.0F) & 0xFF) << 8 | (int)(a.b * 255.0F) & 0xFF;

      for (int i = 0; i < 28; i++) {
         float a0 = (float)((Math.PI * 2) * i / 28.0);
         float a1 = (float)((Math.PI * 2) * (i + 1) / 28.0);
         int r2 = (int)(radius * 0.8F);
         context.batcher
            .box(
               cx + (float)Math.cos(a0) * r2,
               cy + (float)Math.sin(a0) * r2,
               cx + (float)Math.cos(a1) * radius + 1.0F,
               cy + (float)Math.sin(a1) * radius + 1.0F,
               argb
            );
      }

      FontRenderer font = context.batcher.getFont();
      int effect = Math.max(0, Math.min(FxForm.EFFECT_LABELS.length - 1, (Integer)((FxForm)this.form).effect.get()));
      String name = FxForm.EFFECT_LABELS[effect].get();
      context.batcher.textCard(name, cx - font.getWidth(name) / 2, cy - font.getHeight() / 2, c.getARGBColor(), 0);
   }

   @Environment(EnvType.CLIENT)
   private static final class GlState {
      private final boolean depthMask = GL11.glGetBoolean(2930);
      private final boolean blend = GL11.glIsEnabled(3042);
      private final boolean cull = GL11.glIsEnabled(2884);
      private final float[] shaderColor = (float[])RenderSystem.getShaderColor().clone();

      private void restore() {
         RenderSystem.depthFunc(515);
         RenderSystem.depthMask(this.depthMask);
         RenderSystem.setShaderColor(this.shaderColor[0], this.shaderColor[1], this.shaderColor[2], this.shaderColor[3]);
         RenderSystem.defaultBlendFunc();
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
      }
   }
}
