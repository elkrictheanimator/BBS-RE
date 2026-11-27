package bbsfx.client.forms.renderers;

import net.minecraft.client.render.VertexFormat.DrawMode;
import bbsfx.forms.ImageForm;
import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class ImageFormRenderer extends FormRenderer<ImageForm> {
   public ImageFormRenderer(ImageForm form) {
      super(form);
   }

   public void render3D(FormRenderingContext context) {
      if (!BBSRendering.isIrisShadowPass()) {
         ImageForm form = (ImageForm)this.form;
         Link textureLink = (Link)form.texture.get();
         if (textureLink != null) {
            Texture texture = BBSModClient.getTextures().getTexture(textureLink);
            if (texture != null && texture.isValid()) {
               context.stack.push();

               try {
                  if ((Boolean)form.billboard.get()) {
                     Matrix4f modelMatrix = context.stack.peek().getPositionMatrix();
                     Vector3f scale = Vectors.TEMP_3F;
                     modelMatrix.getScale(scale);
                     modelMatrix.m00(1.0F).m01(0.0F).m02(0.0F);
                     modelMatrix.m10(0.0F).m11(1.0F).m12(0.0F);
                     modelMatrix.m20(0.0F).m21(0.0F).m22(1.0F);
                     modelMatrix.scale(scale);
                     context.stack.peek().getNormalMatrix().identity();
                  }

                  this.draw(context, texture);
               } finally {
                  context.stack.pop();
               }
            }
         }
      }
   }

   private void draw(FormRenderingContext context, Texture texture) {
      ImageForm form = (ImageForm)this.form;
      float width = (Float)form.width.get();
      float ratio = (Float)form.aspect.get();
      if (ratio <= 0.0F) {
         ratio = (float)texture.height / Math.max(1, texture.width);
      }

      float height = width * ratio;
      float alpha = (Float)form.opacity.get() * (context.color >>> 24) / 255.0F;
      if (!(alpha <= 0.01F)) {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableCull();
         if ((Boolean)form.throughWalls.get()) {
            RenderSystem.disableDepthTest();
         } else {
            RenderSystem.enableDepthTest();
         }

         texture.bind();
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);
         RenderSystem.setShaderTexture(0, texture.id);
         BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         Matrix4f matrix = context.stack.peek().getPositionMatrix();
         int r = context.color >> 16 & 0xFF;
         int g = context.color >> 8 & 0xFF;
         int b = context.color & 0xFF;
         int a = (int)(alpha * 255.0F);
         float hw = width / 2.0F;
         float hh = height / 2.0F;
         builder.vertex(matrix, -hw, -hh, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
         builder.vertex(matrix, hw, -hh, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
         builder.vertex(matrix, hw, hh, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
         builder.vertex(matrix, -hw, hh, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
         BufferRenderer.drawWithGlobalProgram(builder.end());
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
      }
   }

   public void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
      Link textureLink = (Link)((ImageForm)this.form).texture.get();
      Texture texture = textureLink == null ? null : BBSModClient.getTextures().getTexture(textureLink);
      if (texture != null && texture.isValid()) {
         context.batcher.fullTexturedBox(texture, -1, x1 + 2, y1 + 2, x2 - 2, y2 - 2);
      } else {
         context.batcher.icon(ImageForm.ICON, (x1 + x2) / 2 - 7, (y1 + y2) / 2 - 7);
         context.batcher.outline(x1 + 2, y1 + 2, x2 - 2, y2 - 2, 1090519039);
      }
   }
}
