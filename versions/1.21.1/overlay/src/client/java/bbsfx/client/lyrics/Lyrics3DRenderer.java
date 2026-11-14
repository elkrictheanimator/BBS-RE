package bbsfx.client.lyrics;

import bbsfx.BillboardMath;
import bbsfx.TextGradient;
import bbsfx.Wiggle;
import bbsfx.lyrics.LyricMotion;
import bbsfx.lyrics.LyricPhrases;
import bbsfx.text.Lyrics3DClip;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.clips.CameraClipContext;
import mchorse.bbs_mod.camera.controller.CameraWorkCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.renderers.utils.RenderFrame;
import mchorse.bbs_mod.resources.Link;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public final class Lyrics3DRenderer {
   private static final float BLOCKS_PER_PIXEL = 0.004F;
   private static final float KEEP_AWAY = 0.6F;
   private static final float SMOOTH_SIZE = 10.0F;
   private static final int FULL_BRIGHT = 15728880;
   private static final GlyphBatch BATCH = new GlyphBatch();
   private static final Wiggle WIGGLE = new Wiggle();
   private static Immediate consumers;
   private static long lastEpoch = Long.MIN_VALUE;

   private Lyrics3DRenderer() {
   }

   public static CameraClipContext cameraContext(BaseFilmController controller) {
      if (BBSModClient.getCameraController().getCurrent() instanceof CameraWorkCameraController work && controller.film != null) {
         CameraClipContext context = work.getContext();
         return context.clips == controller.film.camera ? context : null;
      } else {
         return null;
      }
   }

   public static void render(BaseFilmController controller, WorldRenderContext world) {
      if (!BBSRendering.isIrisShadowPass()) {
         CameraClipContext context = cameraContext(controller);
         if (context != null) {
            List<Lyrics3DClip.Frame> frames = Lyrics3DClip.getFrames(context);
            if (!frames.isEmpty()) {
               long epoch = RenderFrame.getEpoch();
               if (epoch != lastEpoch) {
                  lastEpoch = epoch;
                  if (consumers == null) {
                     consumers = VertexConsumerProvider.immediate(new BufferAllocator(4096));
                  }

                  Vec3d camera = world.camera().getPos();
                  Matrix4f view = world.matrixStack().peek().getPositionMatrix();

                  try {
                     glow(frames, view, camera);
                  } catch (Throwable e) {
                     DeepGlow.abort();
                     DeepGlow.disable(e);
                  }

                  for (Lyrics3DClip.Frame frame : frames) {
                     draw(frame, view, camera, false);
                  }

                  ActorLayer.render3D(controller, frames);
                  RenderSystem.enableDepthTest();
                  RenderSystem.depthMask(true);
                  RenderSystem.enableCull();
                  RenderSystem.defaultBlendFunc();
               }
            }
         }
      }
   }

   private static void glow(List<Lyrics3DClip.Frame> frames, Matrix4f view, Vec3d camera) {
      if (DeepGlow.available()) {
         Set<Lyrics3DClip> clips = new LinkedHashSet();

         for (Lyrics3DClip.Frame frame : frames) {
            if ((Boolean)frame.clip.glow.get() && (Float)frame.clip.glowStrength.get() > 0.001F) {
               clips.add(frame.clip);
            }
         }

         for (Lyrics3DClip clip : clips) {
            if (!DeepGlow.begin()) {
               return;
            }

            for (Lyrics3DClip.Frame frame : frames) {
               if (frame.clip == clip) {
                  draw(frame, view, camera, true);
               }
            }

            int tint = (Integer)clip.glowColor.get();
            float pulse = 0.0F;

            for (Lyrics3DClip.Frame frame : frames) {
               if (frame.clip == clip && frame.beatAudio != null) {
                  pulse = BeatCache.pulse(frame.beatAudio, frame.beatMillis) * (Float)clip.beat.get();
                  break;
               }
            }

            DeepGlow.end(
               (tint >> 16 & 0xFF) / 255.0F,
               (tint >> 8 & 0xFF) / 255.0F,
               (tint & 0xFF) / 255.0F,
               (Float)clip.glowStrength.get() * (1.0F + 0.7F * pulse) * 4.0F,
               (Float)clip.glowRadius.get()
            );
         }
      }
   }

   private static void draw(Lyrics3DClip.Frame frame, Matrix4f view, Vec3d camera, boolean glowPass) {
      Lyrics3DClip clip = frame.clip;
      boolean smooth = (Boolean)clip.smoothFont.get();
      SmoothFont smoothFont = smooth ? SmoothFont.get((Integer)clip.fontStyle.get(), (Link)clip.font.get()) : null;
      TextRenderer pixel = MinecraftClient.getInstance().textRenderer;
      float pulse = frame.beatAudio == null ? 0.0F : BeatCache.pulse(frame.beatAudio, frame.beatMillis) * (Float)clip.beat.get();
      double rx = frame.x - camera.x;
      double ry = frame.y + frame.lift - camera.y;
      double rz = frame.z - camera.z;
      double length = Math.sqrt(rx * rx + ry * ry + rz * rz);
      double nearest = (Float)clip.distance.get() * 0.6F;
      if ((Boolean)clip.keepAway.get() && length > 1.0E-4 && length < nearest) {
         double k = nearest / length;
         rx *= k;
         ry *= k;
         rz *= k;
      }

      boolean follow = (Boolean)clip.followCamera.get();
      boolean face = (Boolean)clip.faceCamera.get();
      int orientation = BillboardMath.orientation(follow, face);
      float yaw = 0.0F;
      float pitch = 0.0F;
      float introT = LyricMotion.clamp01(frame.elapsed / 260.0F);
      boolean phraseMode = (Boolean)clip.phraseMode.get();
      int phraseTemplate = (Integer)clip.phraseTemplate.get();
      int introMode = (Integer)clip.intro.get();
      float introShift = phraseMode
         ? LyricPhrases.offset(phraseTemplate, frame.elapsed) * 0.45F
         : (introMode == 1 ? (1.0F - introT) * 0.45F : (introMode == 2 ? -(1.0F - introT) * 0.45F : 0.0F));
      float slideX = !phraseMode && introMode == 5 ? 1.0F - introT : (!phraseMode && introMode == 6 ? -(1.0F - introT) : 0.0F);
      boolean rotate;
      if (follow) {
         float hash = Math.abs(frame.text.hashCode()) % 1000 / 1000.0F;
         float hash2 = Math.abs(frame.text.hashCode() / 7) % 1000 / 1000.0F;
         float distance = (Float)clip.distance.get();
         float sx = (hash - 0.5F) * 0.5F * distance * ((Float)clip.spread.get() / 60.0F);
         float sy = (0.08F - hash2 * 0.22F * ((Float)clip.drop.get() / 40.0F)) * distance;
         Vector3f offset = BillboardMath.screenToWorldOffset(
            view, new Vector3f(sx, -sy + frame.lift + introShift + pulse * 0.08F * (distance / 10.0F), -distance)
         );
         rx = offset.x + (Float)clip.positionX.get();
         ry = offset.y + (Float)clip.positionY.get();
         rz = offset.z + (Float)clip.positionZ.get();
         rotate = orientation != 0;
         if (orientation == 2) {
            yaw = frame.yaw + 180.0F;
         }
      } else if (orientation == 1) {
         yaw = (float)Math.toDegrees(Math.atan2(-rx, rz));
         pitch = (float)Math.toDegrees(Math.atan2(-ry, Math.sqrt(rx * rx + rz * rz)));
         rotate = true;
      } else {
         yaw = frame.yaw + 180.0F;
         pitch = 0.0F;
         rotate = true;
      }

      if (!follow) {
         ry += introShift + pulse * 0.08F * (clip.distance.get() / 10.0F);
      }

      float s = 0.004F * (Float)clip.scale.get() * (Float)clip.distance.get();
      if (phraseMode) {
         s *= LyricPhrases.scale(phraseTemplate, frame.elapsed);
      } else if (introMode == 3) {
         s *= 0.55F + 0.45F * introT;
      } else if (introMode == 7) {
         s *= 1.0F + 0.7F * (1.0F - introT) * (1.0F - introT);
      }

      s *= 1.0F + 0.1F * pulse;
      Matrix4f base = new Matrix4f(view).translate((float)rx, (float)ry, (float)rz);
      if (rotate) {
         base.rotate(new Quaternionf().rotationYXZ((float)Math.toRadians(-yaw), (float)Math.toRadians(pitch), 0.0F));
      }

      base.rotate(
            new Quaternionf()
               .rotationYXZ((float)Math.toRadians(-(Float)clip.yawOffset.get()), (float)Math.toRadians(((Float)clip.pitchOffset.get()).floatValue()), 0.0F)
         )
         .rotateZ((float)Math.toRadians(((Float)clip.roll.get()).floatValue()));
      if (slideX != 0.0F) {
         base.translate(slideX * 30.0F * s, 0.0F, 0.0F);
      }

      base.scale(-s, -s, s);
      float lineHeight = smooth ? 12.0F : 11.0F;
      String lineText = clip.uppercase.get() ? frame.text.toUpperCase(Locale.ROOT) : frame.text;
      List<Lyrics3DRenderer.Glyph> glyphs = layout(
         lineText,
         (Integer)clip.maxWidth.get(),
         lineHeight,
         (Integer)clip.align.get(),
         smooth ? c -> smoothFont.advance(c, 10.0F) : c -> pixel.getWidth(new String(Character.toChars(c)))
      );
      int lines = glyphs.isEmpty() ? 1 : (int)(((Lyrics3DRenderer.Glyph)glyphs.get(glyphs.size() - 1)).y / lineHeight) + 1;
      float translationScale = 0.72F;
      String translation = clip.bilingual.get() && frame.translation != null && !frame.translation.isBlank() ? frame.translation : "";
      List<Lyrics3DRenderer.Glyph> translated = new ArrayList();
      if (!translation.isEmpty()) {
         String translatedText = clip.uppercase.get() ? translation.toUpperCase(Locale.ROOT) : translation;
         float translatedHeight = lineHeight * translationScale;
         translated = layout(
            translatedText,
            (Integer)clip.maxWidth.get(),
            translatedHeight,
            (Integer)clip.align.get(),
            smooth ? c -> smoothFont.advance(c, 10.0F * translationScale) : c -> pixel.getWidth(new String(Character.toChars(c))) * translationScale
         );
      }

      int translatedLines = translated.isEmpty()
         ? 0
         : (int)(((Lyrics3DRenderer.Glyph)translated.get(translated.size() - 1)).y / (lineHeight * translationScale)) + 1;
      float translationGap = translatedLines > 0 ? lineHeight * 0.3F : 0.0F;
      float totalHeight = lines * lineHeight + translationGap + translatedLines * lineHeight * translationScale;
      float top = -totalHeight / 2.0F;
      float translationTop = top + lines * lineHeight + translationGap;
      boolean through = (Boolean)clip.throughWalls.get();
      boolean shadow = (Boolean)clip.textShadow.get() && !glowPass;
      int rgb = (Integer)clip.color.get() & 16777215;
      boolean gradient = (Boolean)clip.gradient.get();
      int gradientFrom = (Integer)clip.gradientFrom.get() & 16777215;
      int gradientTo = (Integer)clip.gradientTo.get() & 16777215;
      int gradientMode = (Integer)clip.gradientMode.get();
      float introAlpha;
      if (phraseMode) {
         introAlpha = LyricPhrases.alpha(phraseTemplate, frame.elapsed);
      } else if (introMode == 4) {
         introAlpha = introT < 1.0F ? 0.2F + 0.8F * Math.abs((float)Math.sin(introT * Math.PI * 3.0)) : 1.0F;
      } else if (introMode != 5 && introMode != 6) {
         introAlpha = introMode != 0 && introMode != 3 && introMode != 7 ? 1.0F : introT;
      } else {
         introAlpha = 1.0F - 0.6F * (1.0F - introT);
      }

      float baseAlpha = ((Integer)clip.color.get() >>> 24) / 255.0F * frame.alpha * introAlpha;
      long wiggleFrame = Wiggle.frame(frame.clock / 1000.0, (Integer)clip.wiggleFps.get());
      float wiggleAmount = clip.wiggle.get() ? (Float)clip.wiggleAmount.get() * (1.0F + 0.8F * pulse) : 0.0F;
      int charFxMode = (Integer)clip.charFx.get();
      Matrix4f matrix = new Matrix4f();
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      if (!through && !glowPass) {
         RenderSystem.enableDepthTest();
      } else {
         RenderSystem.disableDepthTest();
      }

      RenderSystem.depthMask(false);

      for (int pass = 0; pass < (translatedLines > 0 ? 2 : 1); pass++) {
         boolean secondary = pass == 1;
         float fontScale = secondary ? translationScale : 1.0F;
         float currentTop = secondary ? translationTop : top;
         List<Lyrics3DRenderer.Glyph> current = secondary ? translated : glyphs;
         float minX = 0.0F;
         float spanX = 1.0F;
         float minY = 0.0F;
         float spanY = 1.0F;
         if (gradient && !current.isEmpty()) {
            minX = Float.MAX_VALUE;
            float maxX = -Float.MAX_VALUE;
            minY = Float.MAX_VALUE;
            float maxY = -Float.MAX_VALUE;

            for (Lyrics3DRenderer.Glyph glyph : current) {
               minX = Math.min(minX, glyph.x);
               maxX = Math.max(maxX, glyph.x + glyph.width);
               minY = Math.min(minY, glyph.y);
               maxY = Math.max(maxY, glyph.y);
            }

            spanX = Math.max(1.0F, maxX - minX);
            spanY = Math.max(1.0F, maxY - minY);
         }

         for (Lyrics3DRenderer.Glyph glyph : current) {
            float age = frame.elapsed - (float)(!secondary && glyph.index < frame.charTimes.length ? frame.charTimes[glyph.index] : 0L);
            if (!(age < 0.0F)) {
               float alpha = 1.0F;
               float grow = 1.0F;
               float rise = 0.0F;
               float entry = 1.0F;
               if (!secondary && (Boolean)clip.wordPop.get()) {
                  entry = LyricMotion.clamp01(age / 80.0F);
                  alpha = entry;
                  grow = 1.0F + 0.45F * (float)Math.exp(-age / 70.0F);
               } else if (!secondary) {
                  entry = LyricMotion.smoothstep(age / 170.0F);
                  alpha = entry;
                  rise = (1.0F - entry) * 4.0F * fontScale;
               }

               float entrySpin = 0.0F;
               if (!secondary && charFxMode == 1) {
                  grow *= 1.0F + 1.1F * (1.0F - entry);
                  rise = 0.0F;
               } else if (!secondary && charFxMode == 2) {
                  entrySpin = (1.0F - entry) * 90.0F * (glyph.order % 2 == 0 ? 1.0F : -1.0F);
               }

               alpha *= baseAlpha * (secondary ? 0.86F : 1.0F);
               if (!(alpha < 0.02F)) {
                  float x = glyph.x;
                  float y = currentTop + glyph.y;
                  float cx = x + glyph.width / 2.0F;
                  float cy = y + 3.5F * fontScale;
                  WIGGLE.set(glyph.order + (secondary ? 1000 : 0), wiggleFrame, frame.seed, wiggleAmount * fontScale);
                  float glyphScale = (smooth ? 1.0F : fontScale) * WIGGLE.scale * grow;
                  matrix.set(base)
                     .translate(cx + WIGGLE.dx, cy + WIGGLE.dy + rise, 0.0F)
                     .rotateZ((float)Math.toRadians(WIGGLE.angle + entrySpin))
                     .scale(glyphScale, glyphScale, 1.0F)
                     .translate(-cx, -cy, 0.0F);
                  int glyphRgb = rgb;
                  if (gradient) {
                     boolean vertical = gradientMode == 1;
                     glyphRgb = TextGradient.at(
                        gradientFrom, gradientTo, gradientMode, vertical ? glyph.y - minY : glyph.x + glyph.width / 2.0F - minX, vertical ? spanY : spanX
                     );
                  }

                  int argb = (int)(alpha * 255.0F) << 24 | glyphRgb;
                  if (smooth) {
                     if (shadow) {
                        smoothFont.text(
                           BATCH, matrix, glyph.text, x + 0.6F * fontScale, y + 8.6F * fontScale, 10.0F * fontScale, (int)(alpha * 0.45F * 255.0F) << 24
                        );
                     }

                     smoothFont.text(BATCH, matrix, glyph.text, x, y + 8.0F * fontScale, 10.0F * fontScale, argb);
                  } else if (alpha * 255.0F >= 5.0F) {
                     pixel.draw(
                        glyph.text, x, y, argb, shadow, matrix, consumers, !through && !glowPass ? TextLayerType.NORMAL : TextLayerType.SEE_THROUGH, 0, 15728880
                     );
                  }
               }
            }
         }
      }

      consumers.draw();
      BATCH.draw();
   }

   private static List<Lyrics3DRenderer.Glyph> layout(String text, int maxWidth, float lineHeight, int align, IntToDoubleFunction width) {
      List<Lyrics3DRenderer.Glyph> glyphs = new ArrayList();
      List<Lyrics3DRenderer.Glyph> line = new ArrayList();
      float x = 0.0F;
      int lastSpace = -1;
      int row = 0;
      int order = 0;
      int i = 0;

      while (i < text.length()) {
         int codePoint = text.codePointAt(i);
         float w = (float)width.applyAsDouble(codePoint);
         boolean space = Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
         if (!line.isEmpty() && x + w > maxWidth && !space) {
            List<Lyrics3DRenderer.Glyph> carry = new ArrayList();
            if (lastSpace >= 0 && codePoint < 11904) {
               carry.addAll(line.subList(lastSpace + 1, line.size()));
               line = new ArrayList(line.subList(0, lastSpace));
            }

            finish(glyphs, line, row, lineHeight, maxWidth, align);
            row++;
            line = new ArrayList();
            x = 0.0F;

            for (Lyrics3DRenderer.Glyph moved : carry) {
               line.add(new Lyrics3DRenderer.Glyph(moved.text, moved.index, moved.order, x, 0.0F, moved.width));
               x += moved.width;
            }

            lastSpace = -1;
         }

         if (space) {
            if (!line.isEmpty()) {
               lastSpace = line.size();
               line.add(new Lyrics3DRenderer.Glyph(" ", i, -1, x, 0.0F, w));
               x += w;
            }
         } else {
            line.add(new Lyrics3DRenderer.Glyph(new String(Character.toChars(codePoint)), i, order++, x, 0.0F, w));
            x += w;
         }

         i += Character.charCount(codePoint);
      }

      finish(glyphs, line, row, lineHeight, maxWidth, align);
      return glyphs;
   }

   private static void finish(List<Lyrics3DRenderer.Glyph> out, List<Lyrics3DRenderer.Glyph> line, int row, float lineHeight, float maxWidth, int align) {
      while (!line.isEmpty() && ((Lyrics3DRenderer.Glyph)line.get(line.size() - 1)).order < 0) {
         line.remove(line.size() - 1);
      }

      if (!line.isEmpty()) {
         Lyrics3DRenderer.Glyph last = (Lyrics3DRenderer.Glyph)line.get(line.size() - 1);
         float width = last.x + last.width;
         float shift = align == 1 ? 0.0F : (align == 2 ? maxWidth - width : -width / 2.0F);

         for (Lyrics3DRenderer.Glyph glyph : line) {
            if (glyph.order >= 0) {
               out.add(new Lyrics3DRenderer.Glyph(glyph.text, glyph.index, glyph.order, glyph.x + shift, row * lineHeight, glyph.width));
            }
         }
      }
   }

   @Environment(EnvType.CLIENT)
   private record Glyph(String text, int index, int order, float x, float y, float width) {
   }
}
