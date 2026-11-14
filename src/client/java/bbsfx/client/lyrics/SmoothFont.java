package bbsfx.client.lyrics;

import com.mojang.blaze3d.platform.GlStateManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.Link;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public final class SmoothFont {
   public static final int PLAIN = 0;
   public static final int BOLD = 1;
   public static final int DISPLAY = 2;
   private static final int PAD = 4;
   private static final String[] DISPLAY_FAMILIES = new String[]{"Impact", "Anton", "Bebas Neue", "Arial Black", "Segoe UI Black"};
   private static final String[] FAMILIES = new String[]{
      "Microsoft YaHei UI",
      "Microsoft YaHei",
      "PingFang SC",
      "Hiragino Sans GB",
      "Yu Gothic UI",
      "Meiryo UI",
      "Meiryo",
      "Malgun Gothic",
      "Noto Sans CJK SC",
      "Source Han Sans SC",
      "WenQuanYi Micro Hei",
      "SimHei",
      "MS Gothic",
      "Segoe UI Symbol",
      "Segoe UI",
      "Arial Unicode MS"
   };
   private static final Map<String, SmoothFont> FONTS = new HashMap();
   private static Set<String> available;
   private final float base;
   private final int pageSize;
   private final List<Font> fonts;
   private final FontRenderContext context = new FontRenderContext(null, true, true);
   private final Map<Integer, SmoothFont.Glyph> glyphs = new HashMap();
   private final List<SmoothFont.Page> pages = new ArrayList();

   private SmoothFont(int style, Link custom) {
      this.base = style == 2 ? 128.0F : 48.0F;
      this.pageSize = style == 2 ? 2048 : 1024;
      this.fonts = fonts(style, custom, this.base);
   }

   public static SmoothFont get(int style, Link custom) {
      String key = style + "|" + (custom == null ? "" : custom.toString());
      return (SmoothFont)FONTS.computeIfAbsent(key, k -> new SmoothFont(style, custom));
   }

   private static List<Font> fonts(int style, Link custom, float base) {
      List<Font> list = new ArrayList();
      int awtStyle = style == 0 ? 0 : 1;
      if (custom != null) {
         try {
            InputStream stream = BBSMod.getProvider().getAsset(custom);

            try {
               list.add(Font.createFont(0, stream).deriveFont(0, base));
            } catch (Throwable var11) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var10) {
                     var11.addSuppressed(var10);
                  }
               }

               throw var11;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (Exception var12) {
         }
      }

      Set<String> installed = installed();
      if (style == 2) {
         for (String family : DISPLAY_FAMILIES) {
            if (installed.contains(family.toLowerCase(Locale.ROOT))) {
               list.add(new Font(family, 0, 1).deriveFont(base));
            }
         }
      }

      for (String family : FAMILIES) {
         if (installed.contains(family.toLowerCase(Locale.ROOT))) {
            list.add(new Font(family, awtStyle, 1).deriveFont(base));
         }
      }

      list.add(new Font("Dialog", awtStyle, 1).deriveFont(base));
      return list;
   }

   private static Set<String> installed() {
      if (available == null) {
         available = new HashSet();

         try {
            for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames(Locale.ENGLISH)) {
               available.add(family.toLowerCase(Locale.ROOT));
            }
         } catch (Throwable var4) {
         }
      }

      return available;
   }

   public float base() {
      return this.base;
   }

   public float advance(int codePoint, float size) {
      SmoothFont.Glyph glyph = this.glyph(codePoint);
      return glyph == null ? 0.0F : glyph.advance * size / this.base;
   }

   public float width(String text, float size) {
      float width = 0.0F;
      int i = 0;

      while (i < text.length()) {
         int c = text.codePointAt(i);
         i += Character.charCount(c);
         width += this.advance(c, size);
      }

      return width;
   }

   public void glyph(GlyphBatch batch, Matrix4f matrix, int codePoint, float x, float baseline, float size, int argb) {
      SmoothFont.Glyph glyph = this.glyph(codePoint);
      if (glyph != null && glyph.page != null) {
         float k = size / this.base;
         float x0 = x + glyph.left * k;
         float y0 = baseline + glyph.top * k;
         batch.quad(matrix, glyph.page, x0, y0, x0 + glyph.width * k, y0 + glyph.height * k, glyph.u0, glyph.v0, glyph.u1, glyph.v1, argb);
      }
   }

   public void text(GlyphBatch batch, Matrix4f matrix, String text, float x, float baseline, float size, int argb) {
      int i = 0;

      while (i < text.length()) {
         int c = text.codePointAt(i);
         i += Character.charCount(c);
         this.glyph(batch, matrix, c, x, baseline, size, argb);
         x += this.advance(c, size);
      }
   }

   private Font pick(int codePoint) {
      for (Font font : this.fonts) {
         if (font.canDisplay(codePoint)) {
            return font;
         }
      }

      return (Font)this.fonts.get(this.fonts.size() - 1);
   }

   public SmoothFont.Glyph glyph(int codePoint) {
      SmoothFont.Glyph glyph = (SmoothFont.Glyph)this.glyphs.get(codePoint);
      if (glyph == null && !this.glyphs.containsKey(codePoint)) {
         try {
            glyph = this.render(codePoint);
         } catch (Throwable e) {
            glyph = null;
         }

         this.glyphs.put(codePoint, glyph);
      }

      return glyph;
   }

   private SmoothFont.Glyph render(int codePoint) {
      Font font = this.pick(codePoint);
      GlyphVector vector = font.createGlyphVector(this.context, new String(Character.toChars(codePoint)));
      Rectangle bounds = vector.getPixelBounds(this.context, 0.0F, 0.0F);
      SmoothFont.Glyph glyph = new SmoothFont.Glyph();
      glyph.advance = vector.getGlyphMetrics(0).getAdvance();
      if (bounds.width > 0 && bounds.height > 0) {
         int w = bounds.width + 8;
         int h = bounds.height + 8;
         SmoothFont.Page page = this.place(w, h);
         if (page == null) {
            return glyph;
         }

         BufferedImage image = new BufferedImage(w, h, 2);
         Graphics2D graphics = image.createGraphics();
         graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
         graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
         graphics.setColor(Color.WHITE);
         graphics.drawGlyphVector(vector, 4 - bounds.x, 4 - bounds.y);
         graphics.dispose();
         int px = page.x - w - 4;
         this.upload(page, image, px, page.y, w, h);
         glyph.page = page;
         glyph.left = bounds.x - 4;
         glyph.top = bounds.y - 4;
         glyph.width = w;
         glyph.height = h;
         glyph.u0 = (float)px / page.size;
         glyph.v0 = (float)page.y / page.size;
         glyph.u1 = (float)(px + w) / page.size;
         glyph.v1 = (float)(page.y + h) / page.size;
         return glyph;
      } else {
         return glyph;
      }
   }

   private SmoothFont.Page place(int w, int h) {
      if (w + 8 <= this.pageSize && h + 8 <= this.pageSize) {
         SmoothFont.Page page = this.pages.isEmpty() ? null : (SmoothFont.Page)this.pages.get(this.pages.size() - 1);
         if (page != null && page.x + w + 4 > page.size) {
            page.x = 4;
            page.y = page.y + page.row + 4;
            page.row = 0;
         }

         if (page == null || page.y + h + 4 > page.size) {
            page = this.newPage();
         }

         page.x += w + 4;
         page.row = Math.max(page.row, h);
         return page;
      } else {
         return null;
      }
   }

   private SmoothFont.Page newPage() {
      int texture = GlStateManager._genTexture();
      GlStateManager._bindTexture(texture);
      GlStateManager._texParameter(3553, 10241, 9987);
      GlStateManager._texParameter(3553, 10240, 9729);
      GlStateManager._texParameter(3553, 10242, 33071);
      GlStateManager._texParameter(3553, 10243, 33071);
      defaultUnpack();
      IntBuffer clear = MemoryUtil.memCallocInt(this.pageSize * this.pageSize);

      try {
         GlStateManager._texImage2D(3553, 0, 32856, this.pageSize, this.pageSize, 0, 6408, 5121, clear);
      } finally {
         MemoryUtil.memFree(clear);
      }

      SmoothFont.Page page = new SmoothFont.Page(texture, this.pageSize);
      page.mipmapsDirty = true;
      this.pages.add(page);
      return page;
   }

   private void upload(SmoothFont.Page page, BufferedImage image, int x, int y, int w, int h) {
      int[] argb = image.getRGB(0, 0, w, h, null, 0, w);
      IntBuffer buffer = MemoryUtil.memAllocInt(w * h);

      try {
         for (int pixel : argb) {
            buffer.put(pixel & 0xFF000000 | 16777215);
         }

         buffer.flip();
         GlStateManager._bindTexture(page.texture);
         defaultUnpack();
         GL11.glTexSubImage2D(3553, 0, x, y, w, h, 6408, 5121, buffer);
      } finally {
         MemoryUtil.memFree(buffer);
      }

      page.mipmapsDirty = true;
   }

   private static void defaultUnpack() {
      GlStateManager._pixelStore(3314, 0);
      GlStateManager._pixelStore(3316, 0);
      GlStateManager._pixelStore(3315, 0);
      GlStateManager._pixelStore(3317, 4);
   }

   static void prepare(SmoothFont.Page page) {
      if (page.mipmapsDirty) {
         GlStateManager._bindTexture(page.texture);
         GL30.glGenerateMipmap(3553);
         page.mipmapsDirty = false;
      }
   }

   @Environment(EnvType.CLIENT)
   public static final class Glyph {
      SmoothFont.Page page;
      float u0;
      float v0;
      float u1;
      float v1;
      float left;
      float top;
      float width;
      float height;
      float advance;
   }

   @Environment(EnvType.CLIENT)
   static final class Page {
      final int texture;
      final int size;
      int x = 4;
      int y = 4;
      int row;
      boolean mipmapsDirty;

      Page(int texture, int size) {
         this.texture = texture;
         this.size = size;
      }
   }
}
