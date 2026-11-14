package bbsfx.client.lyrics;

import bbsfx.TextGradient;
import bbsfx.lyrics.LyricMotion;
import bbsfx.lyrics.LyricPhrases;
import bbsfx.text.TextFlashClip;
import bbsfx.text.TextFlashLayout;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.UIImageRenderer;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.utils.clips.ClipContext;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import com.mojang.blaze3d.systems.VertexSorter;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

@Environment(EnvType.CLIENT)
public final class TextFlashRenderer {
   private static final float BLOCK_WIDTH = 0.32F;
   private static final float CAP_CENTRE = 0.36F;
   private static final GlyphBatch BATCH = new GlyphBatch();

   private TextFlashRenderer() {
   }

   public static void render(MatrixStack stack, Batcher2D batcher, ClipContext context) {
      List<TextFlashClip.Frame> frames = TextFlashClip.getFrames(context);
      if (!frames.isEmpty()) {
         float width = UIImageRenderer.getUnitWidth();
         float height = 1080.0F;
         Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
         VertexSorter sorting = RenderSystem.getVertexSorting();
         boolean depth = GL11.glIsEnabled(2929);
         int depthFunc = GL11.glGetInteger(2932);
         boolean depthMask = GL11.glGetBoolean(2930);
         boolean masked = ActorLayer.depthMaskActive();
         RenderSystem.setProjectionMatrix(
            masked ? new Matrix4f().ortho(0.0F, width, height, 0.0F, -3000.0F, 1.0F) : new Matrix4f().ortho(0.0F, width, height, 0.0F, -3000.0F, 3000.0F),
            VertexSorter.BY_Z
         );
         if (masked) {
            RenderSystem.enableDepthTest();
            RenderSystem.depthFunc(515);
         } else {
            RenderSystem.disableDepthTest();
         }

         RenderSystem.depthMask(false);
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();

         try {
            Matrix4f base = stack.peek().getPositionMatrix();

            for (TextFlashClip.Frame frame : frames) {
               draw(frame, base, width, height, masked);
            }
         } finally {
            BATCH.clear();
            RenderSystem.setProjectionMatrix(projection, sorting);
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthFunc(depthFunc);
            RenderSystem.depthMask(depthMask);
            RenderSystem.enableCull();
            if (depth) {
               RenderSystem.enableDepthTest();
            } else {
               RenderSystem.disableDepthTest();
            }
         }
      }
   }

   private static void draw(TextFlashClip.Frame frame, Matrix4f base, float width, float height, boolean masked) {
      TextFlashClip clip = frame.clip;
      TextFlashLayout.Block block = frame.block;
      SmoothFont font = SmoothFont.get(2, (Link)clip.font.get());
      boolean current = frame.fade < 0.0F;
      float fade = current ? 0.0F : LyricMotion.clamp01(frame.fade);
      float alpha = frame.alpha * (1.0F - fade);
      boolean phraseMode = (Boolean)clip.phraseMode.get();
      int phraseTemplate = (Integer)clip.phraseTemplate.get();
      float phraseElapsed = frame.now - (float)block.time;
      float phraseScale = phraseMode ? LyricPhrases.scale(phraseTemplate, phraseElapsed) : 1.0F;
      if (phraseMode) {
         alpha *= LyricPhrases.alpha(phraseTemplate, phraseElapsed);
      }

      if (!(alpha <= 0.01F)) {
         float blockWidth = height * 0.32F * (Float)clip.size.get();
         float cx = (Float)clip.x.get() * width;
         float cy = (Float)clip.y.get() * height + (phraseMode ? LyricPhrases.offset(phraseTemplate, phraseElapsed) * 60.0F : 0.0F);
         float pos = frame.now;
         float tracking = (Float)clip.tracking.get();
         float lineHeight = (Float)clip.lineHeight.get();
         float beat = current ? BeatCache.pulse(frame.beatAudio, frame.beatMillis) * (Float)clip.beat.get() : 0.0F;
         float spin = Math.min(30.0F, (Float)clip.beatSpin.get()) * beat;
         List<TextFlashRenderer.Op> ops = new ArrayList();
         TextFlashLayout.Piece slam = current ? block.slamming(pos) : null;
         if (slam != null) {
            float age = pos - (float)slam.start;
            float size = Math.min(blockWidth * 3.0F / Math.max(0.01F, width(font, slam.text, 1.0F, tracking)), height * 0.4F);
            float scale = TextFlashLayout.slamScale(age) * (1.0F + 0.06F * beat) * phraseScale;
            float angle = (float)Math.toRadians(block.tilt + spin);
            Matrix4f matrix = new Matrix4f(base).translate(cx, cy, 0.0F).rotateZ(angle).scale(scale, scale, 1.0F);
            ops.add(
               new TextFlashRenderer.Op(
                  slam.text, matrix, -width(font, slam.text, size, tracking) / 2.0F, size * 0.36F, size, alpha * TextFlashLayout.slamAlpha(age), -1, -1
               )
            );
         } else {
            List<TextFlashLayout.Row> rows = block.visibleRows(pos);
            if (rows.isEmpty()) {
               return;
            }

            int layoutMode = (Integer)clip.layout.get();
            float natural = height * 0.05F * (Float)clip.size.get();
            float[] sizes = new float[rows.size()];
            float gap = blockWidth * 0.025F;
            float total = gap * (rows.size() - 1);

            for (int i = 0; i < rows.size(); i++) {
               sizes[i] = layoutMode == 1
                  ? natural
                  : TextFlashLayout.rowSize((TextFlashLayout.Row)rows.get(i), pos, textx -> width(font, textx, 1.0F, tracking), blockWidth);
               total += sizes[i] * lineHeight;
            }

            long shake = (long)Math.floor(frame.clock / 33.0F);
            float sx = LyricMotion.signed((int)shake, block.time, 51, (Integer)clip.seed.get()) * beat * blockWidth * 0.04F;
            float sy = LyricMotion.signed((int)shake, block.time, 52, (Integer)clip.seed.get()) * beat * blockWidth * 0.04F;
            float grow = (1.0F + 0.09F * beat) * (1.0F + 0.08F * fade) * phraseScale;
            Matrix4f blockMatrix = new Matrix4f(base).translate(cx + sx, cy + sy, 0.0F);
            if (spin > 0.01F) {
               float direction = LyricMotion.signed((int)Math.floor(frame.clock / 250.0F), block.time, 53, (Integer)clip.seed.get());
               blockMatrix.rotateZ((float)Math.toRadians(spin * direction));
            }

            blockMatrix.scale(grow, grow, 1.0F);
            float left = layoutMode == 2 ? -blockWidth / 2.0F : 0.0F;
            float y = -total / 2.0F;
            boolean gradientOn = (Boolean)clip.gradient.get() && !block.red;
            int gradFrom = (Integer)clip.gradientFrom.get() & 16777215;
            int gradTo = (Integer)clip.gradientTo.get() & 16777215;
            boolean gradientVertical = (Integer)clip.gradientMode.get() == 1;

            for (int i = 0; i < rows.size(); i++) {
               int opFrom = -1;
               int opTo = -1;
               if (gradientOn) {
                  if (gradientVertical) {
                     int rowColor = TextGradient.lerp(gradFrom, gradTo, rows.size() == 1 ? 0.0F : (float)i / (rows.size() - 1));
                     opFrom = rowColor;
                     opTo = rowColor;
                  } else {
                     opFrom = gradFrom;
                     opTo = gradTo;
                  }
               }

               TextFlashLayout.Row row = (TextFlashLayout.Row)rows.get(i);
               String text = row.text(pos);
               float size = sizes[i];
               float rowHeight = size * lineHeight;
               float pop = current ? TextFlashLayout.popScale(pos - (float)row.newest(pos)) : 1.0F;
               Matrix4f matrix = new Matrix4f(blockMatrix).translate(left, y + rowHeight / 2.0F, 0.0F).scale(pop, pop, 1.0F);
               ops.add(
                  new TextFlashRenderer.Op(
                     text, matrix, layoutMode == 2 ? 0.0F : -width(font, text, size, tracking) / 2.0F, size * 0.36F, size, alpha, opFrom, opTo
                  )
               );
               y += rowHeight + gap;
            }
         }

         int color = block.red ? (Integer)clip.red.get() : (Integer)clip.white.get();
         float flash = Math.min(1.0F, beat * (Float)clip.beatFlash.get());
         if (flash > 0.001F) {
            color = blend(color, -1, flash);
         }

         TextFlashRenderer.Draw draw = new TextFlashRenderer.Draw(
            font,
            tracking,
            (Integer)clip.outline.get(),
            (Float)clip.outlineWidth.get(),
            (Float)clip.wave.get(),
            (Float)clip.waveSpeed.get(),
            frame.clock,
            1.0F - (Float)clip.strobe.get() * beat * 0.8F
         );
         if (block.glow && (Float)clip.glowStrength.get() > 0.001F) {
            glow(draw, ops, color, (Float)clip.glowStrength.get(), masked);
         }

         draw.emit(ops, color, 1.0F, 1.0F);
      }
   }

   private static float width(SmoothFont font, String text, float size, float tracking) {
      return TextFlashLayout.trackedWidth(text, size, tracking, codePoint -> font.advance(codePoint, 1.0F));
   }

   private static int blend(int color, int target, float t) {
      t = LyricMotion.clamp01(t);
      int a = (int)((color >>> 24 & 0xFF) * (1.0F - t) + (target >>> 24 & 0xFF) * t);
      int r = (int)((color >>> 16 & 0xFF) * (1.0F - t) + (target >>> 16 & 0xFF) * t);
      int g = (int)((color >>> 8 & 0xFF) * (1.0F - t) + (target >>> 8 & 0xFF) * t);
      int b = (int)((color & 0xFF) * (1.0F - t) + (target & 0xFF) * t);
      return a << 24 | r << 16 | g << 8 | b;
   }

   private static void glow(TextFlashRenderer.Draw draw, List<TextFlashRenderer.Op> ops, int color, float strength, boolean masked) {
      if (DeepGlow.available()) {
         try {
            if (DeepGlow.begin()) {
               draw.emit(ops, color, 1.0F, 1.0F);
               DeepGlow.end(1.0F, 1.0F, 1.0F, 1.4F * strength, 0.55F, masked);
               RenderSystem.enableBlend();
               RenderSystem.defaultBlendFunc();
               return;
            }
         } catch (Throwable e) {
            DeepGlow.abort();
            DeepGlow.disable(e);
         }
      }

      RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
      draw.emit(ops, color, 1.06F, 0.22F * strength);
      draw.emit(ops, color, 1.13F, 0.1F * strength);
      RenderSystem.defaultBlendFunc();
   }

   @Environment(EnvType.CLIENT)
   private static final class Draw {
      private final SmoothFont font;
      private final float tracking;
      private final int outline;
      private final float outlineWidth;
      private final float wave;
      private final float waveSpeed;
      private final float clock;
      private final float strobe;

      private Draw(SmoothFont font, float tracking, int outline, float outlineWidth, float wave, float waveSpeed, float clock, float strobe) {
         this.font = font;
         this.tracking = tracking;
         this.outline = outline;
         this.outlineWidth = outlineWidth;
         this.wave = wave;
         this.waveSpeed = waveSpeed;
         this.clock = clock;
         this.strobe = LyricMotion.clamp01(strobe);
      }

      private void emit(List<TextFlashRenderer.Op> ops, int color, float scale, float alpha) {
         for (TextFlashRenderer.Op op : ops) {
            float a = Math.min(1.0F, op.alpha * alpha * this.strobe * ((color >>> 24) / 255.0F));
            int argb = (int)(a * 255.0F) << 24 | color & 16777215;
            Matrix4f matrix = scale == 1.0F ? new Matrix4f(op.matrix) : new Matrix4f(op.matrix).scale(scale, scale, 1.0F);
            float outlineAlpha = Math.min(1.0F, op.alpha * alpha * this.strobe * ((this.outline >>> 24) / 255.0F));
            float offset = this.outlineWidth * op.size * 0.02F;
            if (outlineAlpha > 0.001F && offset > 0.001F) {
               int outlineArgb = (int)(outlineAlpha * 255.0F) << 24 | this.outline & 16777215;
               this.glyphs(op, matrix, outlineArgb, offset);
            }

            this.glyphs(op, matrix, argb, 0.0F);
         }

         TextFlashRenderer.BATCH.draw();
      }

      private void glyphs(TextFlashRenderer.Op op, Matrix4f matrix, int argb, float outline) {
         float x = op.x;
         int index = 0;
         int count = op.gradFrom == -1 ? 1 : op.text.codePointCount(0, op.text.length());
         int i = 0;

         while (i < op.text.length()) {
            int codePoint = op.text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            Matrix4f glyphMatrix = this.wave > 0.001F
               ? new Matrix4f(matrix)
                  .translate(0.0F, (float)Math.sin(this.clock / 1000.0 * this.waveSpeed * 6.0 + index * 0.7) * this.wave * op.size * 0.35F, 0.0F)
               : matrix;
            int glyphArgb = argb;
            if (op.gradFrom != -1) {
               int rgb = TextGradient.at(op.gradFrom, op.gradTo, 0, index, Math.max(1, count - 1));
               glyphArgb = argb & 0xFF000000 | rgb;
            }

            if (outline > 0.0F) {
               float diagonal = outline * 0.70710677F;
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x - outline, op.baseline, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x + outline, op.baseline, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x, op.baseline - outline, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x, op.baseline + outline, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x - diagonal, op.baseline - diagonal, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x - diagonal, op.baseline + diagonal, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x + diagonal, op.baseline - diagonal, op.size, glyphArgb);
               this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x + diagonal, op.baseline + diagonal, op.size, glyphArgb);
            }

            this.font.glyph(TextFlashRenderer.BATCH, glyphMatrix, codePoint, x, op.baseline, op.size, glyphArgb);
            x += this.font.advance(codePoint, op.size) + this.tracking * op.size;
            index++;
            i += charCount;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Op(String text, Matrix4f matrix, float x, float baseline, float size, float alpha, int gradFrom, int gradTo) {
   }
}
