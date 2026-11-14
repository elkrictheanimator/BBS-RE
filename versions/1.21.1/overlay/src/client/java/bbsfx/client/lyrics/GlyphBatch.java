package bbsfx.client.lyrics;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public final class GlyphBatch {
   private final Map<SmoothFont.Page, List<GlyphBatch.Quad>> pages = new LinkedHashMap();

   public boolean isEmpty() {
      return this.pages.isEmpty();
   }

   public void clear() {
      this.pages.clear();
   }

   public void quad(Matrix4f matrix, SmoothFont.Page page, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int argb) {
      if (argb >>> 24 != 0) {
         GlyphBatch.Quad quad = new GlyphBatch.Quad();
         matrix.transformPosition(x0, y0, 0.0F, quad.corners[0]);
         matrix.transformPosition(x0, y1, 0.0F, quad.corners[1]);
         matrix.transformPosition(x1, y1, 0.0F, quad.corners[2]);
         matrix.transformPosition(x1, y0, 0.0F, quad.corners[3]);
         quad.u0 = u0;
         quad.v0 = v0;
         quad.u1 = u1;
         quad.v1 = v1;
         quad.argb = argb;
         ((List)this.pages.computeIfAbsent(page, key -> new ArrayList())).add(quad);
      }
   }

   public void draw() {
      if (!this.pages.isEmpty()) {
         RenderSystem.setShader(GameRenderer::getPositionTexColorProgram);

         for (Entry<SmoothFont.Page, List<GlyphBatch.Quad>> entry : this.pages.entrySet()) {
            SmoothFont.Page page = (SmoothFont.Page)entry.getKey();
            SmoothFont.prepare(page);
            RenderSystem.setShaderTexture(0, page.texture);
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (GlyphBatch.Quad quad : entry.getValue()) {
               int a = quad.argb >>> 24;
               int r = quad.argb >> 16 & 0xFF;
               int g = quad.argb >> 8 & 0xFF;
               int b = quad.argb & 0xFF;
               vertex(builder, quad.corners[0], quad.u0, quad.v0, r, g, b, a);
               vertex(builder, quad.corners[1], quad.u0, quad.v1, r, g, b, a);
               vertex(builder, quad.corners[2], quad.u1, quad.v1, r, g, b, a);
               vertex(builder, quad.corners[3], quad.u1, quad.v0, r, g, b, a);
            }

            BufferRenderer.drawWithGlobalProgram(builder.end());
         }

         this.pages.clear();
      }
   }

   private static void vertex(BufferBuilder builder, Vector3f p, float u, float v, int r, int g, int b, int a) {
      builder.vertex(p.x, p.y, p.z).texture(u, v).color(r, g, b, a);
   }

   @Environment(EnvType.CLIENT)
   private static final class Quad {
      final Vector3f[] corners = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
      float u0;
      float v0;
      float u1;
      float v1;
      int argb;
   }
}
