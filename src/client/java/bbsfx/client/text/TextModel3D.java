package bbsfx.client.text;

import bbsfx.ChatColorCodes;
import bbsfx.TypoMarkup;
import bbsfx.forms.TypoForm;
import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.cubic.render.vao.ModelVAO;
import mchorse.bbs_mod.cubic.render.vao.ModelVAOData;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class TextModel3D {
   private static final int MIN_SS = 4;
   private static final int MAX_SS = 16;
   public final ModelVAO model;
   public final Texture texture;
   public final ModelVAO glowModel;
   public final Texture glowTexture;
   public final int width;
   public final int height;
   public final int supersample;
   public final int padding;
   public final float depth;
   public final String key;

   private TextModel3D(
      ModelVAO model, Texture texture, ModelVAO glowModel, Texture glowTexture, int width, int height, int supersample, int padding, float depth, String key
   ) {
      this.model = model;
      this.texture = texture;
      this.glowModel = glowModel;
      this.glowTexture = glowTexture;
      this.width = width;
      this.height = height;
      this.supersample = supersample;
      this.padding = padding;
      this.depth = depth;
      this.key = key;
   }

   public void delete() {
      if (this.model != null) {
         this.model.delete();
      }

      if (this.texture != null) {
         this.texture.delete();
      }

      if (this.glowModel != null) {
         this.glowModel.delete();
      }

      if (this.glowTexture != null) {
         this.glowTexture.delete();
      }
   }

   public float scale() {
      return (this.width - this.padding * 2) / (16.0F * this.supersample);
   }

   public float contentFraction() {
      return (float)(this.width - this.padding * 2) / this.width;
   }

   private static int supersample(TypoForm form) {
      int fontSize = Math.max(1, (Integer)form.fontSize.get());
      int ss = Math.max(4, Math.min(16, 256 / fontSize));
      int length = ((String)form.text.get()).length();
      if (length > 40) {
         ss = Math.min(ss, 8);
      }

      if (length > 80) {
         ss = Math.min(ss, 4);
      }

      return ss;
   }

   public static String key(TypoForm form) {
      Link font = (Link)form.font.get();
      return (String)form.text.get()
         + "|"
         + form.uppercase.get()
         + "|"
         + (font == null ? "" : font.toString())
         + "|"
         + form.fontSize.get()
         + "|"
         + form.tracking.get()
         + "|"
         + form.lineHeight.get()
         + "|"
         + form.wrap.get()
         + "|"
         + form.align.get()
         + "|"
         + form.depth.get()
         + "|"
         + ((Color)form.outlineColor.get()).getARGBColor()
         + "|"
         + form.outlineWidth.get()
         + "|"
         + form.outlineFollow.get()
         + "|"
         + form.glow.get()
         + "|"
         + form.glowStrength.get()
         + "|"
         + form.glowRadius.get()
         + "|"
         + ((Color)form.glowColor.get()).getARGBColor();
   }

   public static TextModel3D build(TypoForm form) {
      String content = StringUtils.processColoredText((String)form.text.get());
      if ((Boolean)form.uppercase.get()) {
         content = content.toUpperCase();
      }

      List<List<TypoMarkup.Glyph>> lines = lines(TypoMarkup.parse(content), (Integer)form.wrap.get());
      return build(form, lines, null, false, key(form));
   }

   public static TextModel3D buildGlyph(TypoForm form, TypoMarkup.Glyph glyph, TypoMarkup.Glyph following) {
      if (Character.isSpaceChar(glyph.codePoint)) {
         return null;
      }

      List<List<TypoMarkup.Glyph>> lines = new ArrayList();
      lines.add(Collections.singletonList(glyph));
      String key = key(form) + "|glyph|" + glyph.codePoint + "|" + glyph.rgb + "|" + glyph.style + "|" + (following == null ? 0 : following.style);
      return build(form, lines, following, true, key);
   }

   private static TextModel3D build(TypoForm form, List<List<TypoMarkup.Glyph>> lines, TypoMarkup.Glyph following, boolean singleGlyph, String key) {
      int ss = supersample(form);
      Font font = loadFont((Link)form.font.get(), Math.max(6, (Integer)form.fontSize.get()) * ss);
      BufferedImage probe = new BufferedImage(1, 1, 2);
      Graphics2D probeGraphics = probe.createGraphics();
      probeGraphics.setFont(font);
      FontMetrics metrics = probeGraphics.getFontMetrics();
      LineMetrics lineMetrics = font.getLineMetrics("Ag", probeGraphics.getFontRenderContext());
      probeGraphics.dispose();
      float tracking = (Float)form.tracking.get() * ss;
      float bold = ss;
      int lineHeight = Math.max(metrics.getHeight(), form.lineHeight.get() > 0.0F ? Math.round((Float)form.lineHeight.get() * ss) : metrics.getHeight());
      int maxWidth = 0;

      for (List<TypoMarkup.Glyph> line : lines) {
         maxWidth = Math.max(maxWidth, measure(metrics, line, tracking, bold));
      }

      if (singleGlyph && following != null && tracking > 0.0F) {
         TypoMarkup.Glyph glyph = (TypoMarkup.Glyph)((List)lines.get(0)).get(0);
         if (glyph.has(8) && following.has(8) || glyph.has(4) && following.has(4)) {
            maxWidth += (int)Math.ceil(tracking);
         }
      }

      int width = Math.max(1, maxWidth + 8);
      int height = Math.max(1, lines.size() * lineHeight + 8);
      int padding = Math.max(8, Math.round(Math.max(6, (Integer)form.fontSize.get()) * ss * 0.6F));
      width = maxWidth + padding * 2;
      height = lines.size() * lineHeight + padding * 2;
      BufferedImage image = new BufferedImage(width, height, 2);
      Graphics2D graphics = image.createGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      graphics.setFont(font);
      graphics.setColor(java.awt.Color.WHITE);
      Color outline = (Color)form.outlineColor.get();
      float outlineWidth = Math.max(0.0F, (Float)form.outlineWidth.get()) * ss;
      boolean outlined = outlineWidth > 0.0F && outline.a > 0.0F;
      BasicStroke stroke = new BasicStroke(outlineWidth, 1, 1);
      FontRenderContext frc = graphics.getFontRenderContext();
      float underlineOffset = lineMetrics.getUnderlineOffset();
      float underlineThickness = Math.max(1.0F, lineMetrics.getUnderlineThickness());
      float strikeOffset = lineMetrics.getStrikethroughOffset();
      float strikeThickness = Math.max(1.0F, lineMetrics.getStrikethroughThickness());
      int baseline = padding + metrics.getAscent();
      float align = singleGlyph ? 0.0F : ((Integer)form.align.get()).intValue() / 2.0F;

      for (List<TypoMarkup.Glyph> line : lines) {
         float x = padding + (maxWidth - measure(metrics, line, tracking, bold)) * align;

         for (int i = 0; i < line.size(); i++) {
            TypoMarkup.Glyph glyph = (TypoMarkup.Glyph)line.get(i);
            TypoMarkup.Glyph next = singleGlyph ? following : (i + 1 < line.size() ? (TypoMarkup.Glyph)line.get(i + 1) : null);
            float advance = advance(metrics, glyph, bold);
            List<Shape> shapes = new ArrayList();
            if (!Character.isSpaceChar(glyph.codePoint)) {
               Shape shape = font.createGlyphVector(frc, glyph.text()).getOutline(x, baseline);
               if (glyph.has(16)) {
                  shape = italic(shape, baseline);
               }

               shapes.add(shape);
               if (glyph.has(2)) {
                  shapes.add(AffineTransform.getTranslateInstance(bold, 0.0).createTransformedShape(shape));
               }
            }

            if (glyph.has(8)) {
               float span = advance + (next != null && next.has(8) ? tracking : 0.0F);
               shapes.add(new java.awt.geom.Rectangle2D.Float(x, baseline + underlineOffset, span, underlineThickness));
            }

            if (glyph.has(4)) {
               float span = advance + (next != null && next.has(4) ? tracking : 0.0F);
               shapes.add(new java.awt.geom.Rectangle2D.Float(x, baseline + strikeOffset - strikeThickness / 2.0F, span, strikeThickness));
            }

            if (outlined && !shapes.isEmpty()) {
               graphics.setColor(outlineColor(outline, glyph, (Boolean)form.outlineFollow.get()));
               graphics.setStroke(stroke);

               for (Shape shape : shapes) {
                  graphics.draw(shape);
               }
            }

            graphics.setColor(glyph.rgb == -1 ? java.awt.Color.WHITE : new java.awt.Color(glyph.rgb));

            for (Shape shape : shapes) {
               graphics.fill(shape);
            }

            x += advance + tracking;
         }

         baseline += lineHeight;
      }

      graphics.dispose();
      if ((Boolean)form.glow.get() && (Float)form.glowStrength.get() > 0.0F) {
         image = withGlow(image, form);
      }

      Pixels pixels = toPixels(image, width, height);
      float depth = Math.max(0.0F, (Float)form.depth.get()) * ss / Math.max(1, width - padding * 2);
      ModelVAOData data = extrude(pixels, depth);
      if (data == null) {
         pixels.delete();
         return null;
      } else {
         pixels.getBuffer().clear();
         ModelVAO model = new ModelVAO(data);
         Texture texture = Texture.textureFromPixels(pixels, 9729);
         ModelVAO glowModel = null;
         Texture glowTexture = null;
         return new TextModel3D(model, texture, glowModel, glowTexture, width, height, ss, padding, depth, key);
      }
   }

   private static BufferedImage withGlow(BufferedImage image, TypoForm form) {
      int width = image.getWidth();
      int height = image.getHeight();
      float[] alpha = new float[width * height];

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            alpha[y * width + x] = (image.getRGB(x, y) >>> 24 & 0xFF) / 255.0F;
         }
      }

      int radius = Math.max(2, Math.round(height * (0.02F + 0.05F * (Float)form.glowRadius.get())));
      radius = Math.min(64, radius);
      float[] buffer = new float[alpha.length];

      for (int pass = 0; pass < 3; pass++) {
         blurHorizontal(alpha, buffer, width, height, radius);
         blurVertical(buffer, alpha, width, height, radius);
      }

      Color glow = (Color)form.glowColor.get();
      float strength = (Float)form.glowStrength.get();
      int rgb = (int)(glow.r * 255.0F) << 16 | (int)(glow.g * 255.0F) << 8 | (int)(glow.b * 255.0F);
      BufferedImage halo = new BufferedImage(width, height, 2);

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int a = (int)(Math.min(1.0F, alpha[y * width + x] * strength) * 255.0F);
            halo.setRGB(x, y, a << 24 | rgb);
         }
      }

      BufferedImage combined = new BufferedImage(width, height, 2);
      Graphics2D graphics = combined.createGraphics();
      graphics.drawImage(halo, 0, 0, null);
      graphics.drawImage(image, 0, 0, null);
      graphics.dispose();
      return combined;
   }

   private static void blurHorizontal(float[] src, float[] dst, int width, int height, int radius) {
      float norm = 1.0F / (radius * 2 + 1);

      for (int y = 0; y < height; y++) {
         int row = y * width;
         float sum = 0.0F;

         for (int i = -radius; i <= radius; i++) {
            sum += src[row + Math.max(0, Math.min(width - 1, i))];
         }

         for (int x = 0; x < width; x++) {
            dst[row + x] = sum * norm;
            sum += src[row + Math.min(width - 1, x + radius + 1)] - src[row + Math.max(0, x - radius)];
         }
      }
   }

   private static void blurVertical(float[] src, float[] dst, int width, int height, int radius) {
      float norm = 1.0F / (radius * 2 + 1);

      for (int x = 0; x < width; x++) {
         float sum = 0.0F;

         for (int i = -radius; i <= radius; i++) {
            sum += src[Math.max(0, Math.min(height - 1, i)) * width + x];
         }

         for (int y = 0; y < height; y++) {
            dst[y * width + x] = sum * norm;
            sum += src[Math.min(height - 1, y + radius + 1) * width + x] - src[Math.max(0, y - radius) * width + x];
         }
      }
   }

   private static ModelVAO quadModel(int width, int height) {
      float aspect = (float)height / width;
      float hx = 0.5F;
      float hy = 0.5F * aspect;
      List<Float> vertices = new ArrayList();
      List<Float> normals = new ArrayList();
      List<Float> uvs = new ArrayList();
      add(vertices, normals, uvs, hx, -hy, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      add(vertices, normals, uvs, -hx, -hy, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      add(vertices, normals, uvs, -hx, hy, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
      add(vertices, normals, uvs, hx, -hy, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      add(vertices, normals, uvs, -hx, hy, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
      add(vertices, normals, uvs, hx, hy, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F);
      float[] v = toArray(vertices);
      float[] n = toArray(normals);
      float[] u = toArray(uvs);
      float[] t = BBSRendering.calculateTangents(v, n, u);
      return new ModelVAO(new ModelVAOData(v, n, t, u));
   }

   private static Font loadFont(Link link, int size) {
      if (link != null) {
         try {
            InputStream stream = BBSMod.getProvider().getAsset(link);

            Font var3;
            try {
               var3 = Font.createFont(0, stream).deriveFont(0, size);
            } catch (Throwable var6) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (stream != null) {
               stream.close();
            }

            return var3;
         } catch (Exception var7) {
         }
      }

      return new Font("SansSerif", 0, size);
   }

   private static List<List<TypoMarkup.Glyph>> lines(List<List<TypoMarkup.Glyph>> parsed, int wrap) {
      List<List<TypoMarkup.Glyph>> lines = new ArrayList();

      for (List<TypoMarkup.Glyph> raw : parsed) {
         if (wrap > 0 && raw.size() > wrap) {
            for (int i = 0; i < raw.size(); i += wrap) {
               lines.add(raw.subList(i, Math.min(raw.size(), i + wrap)));
            }
         } else {
            lines.add(raw);
         }
      }

      if (lines.isEmpty()) {
         lines.add(new ArrayList());
      }

      return lines;
   }

   private static float advance(FontMetrics metrics, TypoMarkup.Glyph glyph, float bold) {
      return metrics.charWidth(glyph.codePoint) + (glyph.has(2) ? bold : 0.0F);
   }

   private static int measure(FontMetrics metrics, List<TypoMarkup.Glyph> line, float tracking, float bold) {
      float width = 0.0F;

      for (TypoMarkup.Glyph glyph : line) {
         width += advance(metrics, glyph, bold) + tracking;
      }

      return Math.max(0, (int)(width - (line.isEmpty() ? 0.0F : tracking)));
   }

   private static Shape italic(Shape shape, float baseline) {
      AffineTransform transform = AffineTransform.getTranslateInstance(0.0, baseline);
      transform.shear(-0.25, 0.0);
      transform.translate(0.0, -baseline);
      return transform.createTransformedShape(shape);
   }

   private static java.awt.Color outlineColor(Color outline, TypoMarkup.Glyph glyph, boolean follow) {
      int alpha = Math.round(outline.a * 255.0F);
      if (follow && glyph.rgb != -1) {
         int rgb = ChatColorCodes.shade(glyph.rgb, 0.4F);
         return new java.awt.Color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha);
      } else {
         return new java.awt.Color(outline.r, outline.g, outline.b, outline.a);
      }
   }

   private static Pixels toPixels(BufferedImage image, int width, int height) {
      Pixels pixels = Pixels.fromSize(width, height);
      Color color = new Color();

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            int argb = image.getRGB(x, y);
            color.set((argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, (argb >> 24 & 0xFF) / 255.0F);
            pixels.setColor(x, y, color);
         }
      }

      pixels.getBuffer().clear();
      return pixels;
   }

   private static ModelVAOData extrude(Pixels pixels, float depth) {
      List<Float> vertices = new ArrayList();
      List<Float> normals = new ArrayList();
      List<Float> uvs = new ArrayList();
      float px = 0.5F;
      float py = 0.5F;
      if (pixels.width > pixels.height) {
         py = (float)pixels.height / pixels.width * 0.5F;
      } else if (pixels.height > pixels.width) {
         px = (float)pixels.width / pixels.height * 0.5F;
      }

      float nx = -px;
      float ny = -py;
      fillQuad(vertices, normals, uvs, px, ny, depth, nx, ny, depth, nx, py, depth, px, py, depth, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F);
      fillQuad(vertices, normals, uvs, nx, ny, -depth, px, ny, -depth, px, py, -depth, nx, py, -depth, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, -1.0F);

      for (int y = 0; y < pixels.height; y++) {
         for (int x = 0; x < pixels.width; x++) {
            if (hasPixel(pixels, x, y)) {
               sides(pixels, vertices, normals, uvs, px, py, x, y, depth);
            }
         }
      }

      if (vertices.isEmpty()) {
         return null;
      }

      float[] v = toArray(vertices);
      float[] n = toArray(normals);
      float[] u = toArray(uvs);
      float[] t = BBSRendering.calculateTangents(v, n, u);
      return new ModelVAOData(v, n, t, u);
   }

   private static void sides(Pixels pixels, List<Float> vertices, List<Float> normals, List<Float> uvs, float px, float py, int x, int y, float depth) {
      float w = pixels.width;
      float h = pixels.height;
      float sx = 1.0F / w * (px / 0.5F);
      float sy = 1.0F / h * (py / 0.5F);
      float u = (x + 0.5F) / w;
      float v = (y + 0.5F) / h;
      if (!hasPixel(pixels, x - 1, y) || x == 0) {
         fillQuad(
            vertices,
            normals,
            uvs,
            x * sx - px,
            -(y + 1) * sy + py,
            -depth,
            x * sx - px,
            -y * sy + py,
            -depth,
            x * sx - px,
            -y * sy + py,
            depth,
            x * sx - px,
            -(y + 1) * sy + py,
            depth,
            u,
            v,
            u,
            v,
            -1.0F,
            0.0F,
            0.0F
         );
      }

      if (!hasPixel(pixels, x + 1, y) || x == w - 1.0F) {
         fillQuad(
            vertices,
            normals,
            uvs,
            (x + 1) * sx - px,
            -(y + 1) * sy + py,
            depth,
            (x + 1) * sx - px,
            -y * sy + py,
            depth,
            (x + 1) * sx - px,
            -y * sy + py,
            -depth,
            (x + 1) * sx - px,
            -(y + 1) * sy + py,
            -depth,
            u,
            v,
            u,
            v,
            1.0F,
            0.0F,
            0.0F
         );
      }

      if (!hasPixel(pixels, x, y - 1) || y == 0) {
         fillQuad(
            vertices,
            normals,
            uvs,
            (x + 1) * sx - px,
            -y * sy + py,
            depth,
            x * sx - px,
            -y * sy + py,
            depth,
            x * sx - px,
            -y * sy + py,
            -depth,
            (x + 1) * sx - px,
            -y * sy + py,
            -depth,
            u,
            v,
            u,
            v,
            0.0F,
            1.0F,
            0.0F
         );
      }

      if (!hasPixel(pixels, x, y + 1) || y == h - 1.0F) {
         fillQuad(
            vertices,
            normals,
            uvs,
            (x + 1) * sx - px,
            -(y + 1) * sy + py,
            -depth,
            x * sx - px,
            -(y + 1) * sy + py,
            -depth,
            x * sx - px,
            -(y + 1) * sy + py,
            depth,
            (x + 1) * sx - px,
            -(y + 1) * sy + py,
            depth,
            u,
            v,
            u,
            v,
            0.0F,
            -1.0F,
            0.0F
         );
      }
   }

   private static boolean hasPixel(Pixels pixels, int x, int y) {
      Color color = pixels.getColor(x, y);
      return color != null && color.a > 0.5F;
   }

   private static void fillQuad(
      List<Float> vertices,
      List<Float> normals,
      List<Float> uvs,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float x4,
      float y4,
      float z4,
      float u1,
      float v1,
      float u2,
      float v2,
      float nx,
      float ny,
      float nz
   ) {
      add(vertices, normals, uvs, x2, y2, z2, u1, v2, nx, ny, nz);
      add(vertices, normals, uvs, x1, y1, z1, u2, v2, nx, ny, nz);
      add(vertices, normals, uvs, x4, y4, z4, u2, v1, nx, ny, nz);
      add(vertices, normals, uvs, x2, y2, z2, u1, v2, nx, ny, nz);
      add(vertices, normals, uvs, x4, y4, z4, u2, v1, nx, ny, nz);
      add(vertices, normals, uvs, x3, y3, z3, u1, v1, nx, ny, nz);
   }

   private static void add(
      List<Float> vertices, List<Float> normals, List<Float> uvs, float x, float y, float z, float u, float v, float nx, float ny, float nz
   ) {
      vertices.add(x);
      vertices.add(y);
      vertices.add(z);
      normals.add(nx);
      normals.add(ny);
      normals.add(nz);
      uvs.add(u);
      uvs.add(v);
   }

   private static float[] toArray(List<Float> list) {
      float[] array = new float[list.size()];

      for (int i = 0; i < array.length; i++) {
         array[i] = (Float)list.get(i);
      }

      return array;
   }
}
