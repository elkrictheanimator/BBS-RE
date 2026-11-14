package bbsfx.client.lyrics;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

@Environment(EnvType.CLIENT)
public final class DeepGlow {
   private static final int LEVELS = 7;
   private static ShaderProgram down;
   private static ShaderProgram up;
   private static ShaderProgram composite;
   private static boolean failed;
   private static boolean active;
   private static int width;
   private static int height;
   private static int maskTexture;
   private static int maskFbo;
   private static final int[] levelTexture = new int[7];
   private static final int[] levelFbo = new int[7];
   private static final int[] levelWidth = new int[7];
   private static final int[] levelHeight = new int[7];
   private static int savedFbo;
   private static final int[] savedViewport = new int[4];
   private static final float[] savedClear = new float[4];
   private static boolean savedDepthTest;
   private static boolean savedDepthMask;
   private static boolean savedCull;
   private static boolean savedBlend;

   private DeepGlow() {
   }

   public static void registerShaders() {
      CoreShaderRegistrationCallback.EVENT.register((CoreShaderRegistrationCallback)context -> {
         context.register(new Identifier("bbsfx", "glow_down"), VertexFormats.POSITION_TEXTURE, program -> down = program);
         context.register(new Identifier("bbsfx", "glow_up"), VertexFormats.POSITION_TEXTURE, program -> up = program);
         context.register(new Identifier("bbsfx", "glow_composite"), VertexFormats.POSITION_TEXTURE, program -> composite = program);
      });
   }

   public static boolean available() {
      return !failed && down != null && up != null && composite != null;
   }

   public static void disable(Throwable reason) {
      if (!failed) {
         System.err.println("[bbsfx] Deep Glow disabled: " + reason);
      }

      failed = true;
      abort();
      release();
   }

   public static boolean begin() {
      if (!available()) {
         return false;
      }

      GL11.glGetIntegerv(2978, savedViewport);
      int w = savedViewport[2];
      int h = savedViewport[3];
      if (w > 0 && h > 0) {
         savedFbo = GlStateManager._getInteger(36006);
         savedDepthTest = GL11.glIsEnabled(2929);
         savedDepthMask = GL11.glGetBoolean(2930);
         savedCull = GL11.glIsEnabled(2884);
         savedBlend = GL11.glIsEnabled(3042);
         GL11.glGetFloatv(3106, savedClear);
         active = true;

         try {
            ensureTargets(w, h);
            GlStateManager._glBindFramebuffer(36160, maskFbo);
            RenderSystem.viewport(0, 0, w, h);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.clear(16384, false);
            RenderSystem.clearColor(savedClear[0], savedClear[1], savedClear[2], savedClear[3]);
            return true;
         } catch (RuntimeException | Error e) {
            abort();
            throw e;
         }
      } else {
         return false;
      }
   }

   public static void end(float r, float g, float b, float exposure, float spread) {
      end(r, g, b, exposure, spread, false);
   }

   public static void end(float r, float g, float b, float exposure, float spread, boolean depthMasked) {
      if (active) {
         try {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.disableBlend();
            int source = maskTexture;
            int sourceW = width;
            int sourceH = height;

            for (int i = 0; i < 7; i++) {
               GlStateManager._glBindFramebuffer(36160, levelFbo[i]);
               RenderSystem.viewport(0, 0, levelWidth[i], levelHeight[i]);
               set(down, "Texel", 1.0F / sourceW, 1.0F / sourceH);
               pass(down, source);
               source = levelTexture[i];
               sourceW = levelWidth[i];
               sourceH = levelHeight[i];
            }

            RenderSystem.enableBlend();
            RenderSystem.blendFunc(SrcFactor.ONE, DstFactor.ONE);
            if (up.getUniform("Intensity") != null) {
               up.getUniform("Intensity").set(0.55F + Math.max(0.0F, Math.min(1.0F, spread)) * 0.6F);
            }

            for (int i = 5; i >= 0; i--) {
               GlStateManager._glBindFramebuffer(36160, levelFbo[i]);
               RenderSystem.viewport(0, 0, levelWidth[i], levelHeight[i]);
               set(up, "Texel", 1.0F / levelWidth[i + 1], 1.0F / levelHeight[i + 1]);
               pass(up, levelTexture[i + 1]);
            }

            GlStateManager._glBindFramebuffer(36160, savedFbo);
            RenderSystem.viewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
            if (composite.getUniform("Tint") != null) {
               composite.getUniform("Tint").set(r, g, b);
            }

            if (composite.getUniform("Exposure") != null) {
               composite.getUniform("Exposure").set(exposure);
            }

            float[] depthRange = new float[2];
            if (depthMasked) {
               GL11.glGetFloatv(2928, depthRange);
               RenderSystem.enableDepthTest();
               RenderSystem.depthFunc(515);
               RenderSystem.depthMask(false);
               GL11.glDepthRange(1.0, 1.0);
            }

            try {
               pass(composite, levelTexture[0]);
            } finally {
               if (depthMasked) {
                  GL11.glDepthRange(depthRange[0], depthRange[1]);
               }
            }
         } catch (RuntimeException | Error e) {
            abort();
            throw e;
         }

         restoreState();
         active = false;
      }
   }

   public static void abort() {
      if (active) {
         restoreState();
         active = false;
      }
   }

   private static void restoreState() {
      GlStateManager._glBindFramebuffer(36160, savedFbo);
      RenderSystem.viewport(savedViewport[0], savedViewport[1], savedViewport[2], savedViewport[3]);
      RenderSystem.clearColor(savedClear[0], savedClear[1], savedClear[2], savedClear[3]);
      RenderSystem.depthMask(savedDepthMask);
      if (savedDepthTest) {
         RenderSystem.enableDepthTest();
      } else {
         RenderSystem.disableDepthTest();
      }

      if (savedCull) {
         RenderSystem.enableCull();
      } else {
         RenderSystem.disableCull();
      }

      if (savedBlend) {
         RenderSystem.enableBlend();
      } else {
         RenderSystem.disableBlend();
      }
   }

   private static void set(ShaderProgram program, String name, float x, float y) {
      if (program.getUniform(name) != null) {
         program.getUniform(name).set(x, y);
      }
   }

   private static void pass(ShaderProgram program, int texture) {
      RenderSystem.setShader(() -> program);
      RenderSystem.setShaderTexture(0, texture);
      BufferBuilder builder = Tessellator.getInstance().getBuffer();
      builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
      builder.vertex(-1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).next();
      builder.vertex(1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).next();
      builder.vertex(1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).next();
      builder.vertex(-1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).next();
      BufferRenderer.drawWithGlobalProgram(builder.end());
   }

   private static void ensureTargets(int w, int h) {
      if (w != width || h != height || maskFbo == 0) {
         release();
         width = w;
         height = h;
         maskTexture = texture(w, h, false);
         maskFbo = framebuffer(maskTexture);
         int lw = w;
         int lh = h;

         for (int i = 0; i < 7; i++) {
            lw = Math.max(1, lw / 2);
            lh = Math.max(1, lh / 2);
            levelWidth[i] = lw;
            levelHeight[i] = lh;
            levelTexture[i] = texture(lw, lh, true);
            levelFbo[i] = framebuffer(levelTexture[i]);
         }

         GlStateManager._glBindFramebuffer(36160, savedFbo);
      }
   }

   private static int texture(int w, int h, boolean hdr) {
      int id = GlStateManager._genTexture();
      GlStateManager._bindTexture(id);
      GlStateManager._texParameter(3553, 10241, 9729);
      GlStateManager._texParameter(3553, 10240, 9729);
      GlStateManager._texParameter(3553, 10242, 33071);
      GlStateManager._texParameter(3553, 10243, 33071);
      GlStateManager._texImage2D(3553, 0, hdr ? 34842 : 32856, w, h, 0, 6408, hdr ? 5126 : 5121, null);
      return id;
   }

   private static int framebuffer(int texture) {
      int fbo = GlStateManager.glGenFramebuffers();
      GlStateManager._glBindFramebuffer(36160, fbo);
      GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, texture, 0);
      int status = GL30.glCheckFramebufferStatus(36160);
      if (status != 36053) {
         GlStateManager._glDeleteFramebuffers(fbo);
         throw new IllegalStateException("Incomplete Deep Glow framebuffer: 0x" + Integer.toHexString(status));
      } else {
         return fbo;
      }
   }

   private static void release() {
      if (maskTexture != 0) {
         GlStateManager._deleteTexture(maskTexture);
         GlStateManager._glDeleteFramebuffers(maskFbo);
      }

      for (int i = 0; i < 7; i++) {
         if (levelTexture[i] != 0) {
            GlStateManager._deleteTexture(levelTexture[i]);
            GlStateManager._glDeleteFramebuffers(levelFbo[i]);
         }

         levelTexture[i] = 0;
         levelFbo[i] = 0;
      }

      maskTexture = 0;
      maskFbo = 0;
   }
}
