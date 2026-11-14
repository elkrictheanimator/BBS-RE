package bbsfx.client.forms.renderers;

import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.render.VertexFormat.DrawMode;
import bbsfx.BezierEasing;
import bbsfx.BillboardMath;
import bbsfx.ChatColorCodes;
import bbsfx.TextAnimOrder;
import bbsfx.TextGradient;
import bbsfx.TimeRemap;
import bbsfx.TypoMarkup;
import bbsfx.client.text.TextModel3D;
import bbsfx.forms.TypoForm;
import com.mojang.blaze3d.platform.GlStateManager.DstFactor;
import com.mojang.blaze3d.platform.GlStateManager.SrcFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.BBSShaders;
import mchorse.bbs_mod.cubic.render.vao.ModelVAORenderer;
import mchorse.bbs_mod.forms.CustomVertexConsumerProvider;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.ITickable;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.renderers.FormRenderer;
import mchorse.bbs_mod.forms.renderers.FormRenderingContext;
import mchorse.bbs_mod.forms.renderers.utils.FormColorBlend;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.utils.MatrixStackUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.OverlayBlend;
import mchorse.bbs_mod.utils.joml.Vectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Style;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.text.TextColor;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.OrderedText;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.render.VertexFormat;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@Environment(EnvType.CLIENT)
public class TypoFormRenderer extends FormRenderer<TypoForm> implements ITickable {
   private long ticks;
   private TypoFormRenderer.Layout cachedLayout;
   private FontRenderer cachedFont;
   private String cachedContent;
   private float cachedTracking;
   private float cachedLineHeight;
   private int cachedWrap;
   private int cachedAnimUnit = -1;
   private boolean cachedGradient;
   private int cachedGradientFrom = -1;
   private int cachedGradientTo = -1;
   private int cachedGradientMode = -1;
   private TextModel3D textModel;
   private String textModelKey;
   private final Map<String, TextModel3D> textGlyphModels = new HashMap();
   private String textGlyphModelsKey;
   private final List<TypoFormRenderer.Decoration> decorations = new ArrayList();
   private boolean decorating;
   private float decorationBaseline;
   private float decorationThickness;

   public TypoFormRenderer(TypoForm form) {
      super(form);
   }

   public void tick(IEntity entity) {
      this.ticks++;
   }

   private float clock(FormRenderingContext context) {
      if (context.modelRenderer) {
         return (float)context.modelRendererTick + context.getTransition();
      }

      Float filmTime = FilmClock.time(context.entity, context.getTransition());
      return filmTime == null ? (float)this.ticks + context.getTransition() : filmTime;
   }

   private FontRenderer getFont(float scale) {
      FontRenderer font = BBSModClient.getFonts().get((Link)((TypoForm)this.form).font.get(), (Integer)((TypoForm)this.form).fontSize.get(), scale);
      return font == null ? Batcher2D.getDefaultTextRenderer() : font;
   }

   private static float baseline(FontRenderer font) {
      return font == Batcher2D.getDefaultTextRenderer() ? font.getHeight() : font.getHeight() - 3.0F;
   }

   private String content() {
      String text = StringUtils.processColoredText((String)((TypoForm)this.form).text.get());
      return ((TypoForm)this.form).uppercase.get() ? text.toUpperCase() : text;
   }

   private TypoFormRenderer.Layout layout(FontRenderer font) {
      String content = this.content();
      float tracking = (Float)((TypoForm)this.form).tracking.get();
      float lineHeight = (Float)((TypoForm)this.form).lineHeight.get();
      int wrap = (Integer)((TypoForm)this.form).wrap.get();
      boolean gradient = (Boolean)((TypoForm)this.form).gradient.get();
      int gradientFrom = ((Color)((TypoForm)this.form).gradientFrom.get()).getARGBColor() & 16777215;
      int gradientTo = ((Color)((TypoForm)this.form).gradientTo.get()).getARGBColor() & 16777215;
      int gradientMode = (Integer)((TypoForm)this.form).gradientMode.get();
      if (this.cachedLayout != null
         && font == this.cachedFont
         && content.equals(this.cachedContent)
         && tracking == this.cachedTracking
         && lineHeight == this.cachedLineHeight
         && wrap == this.cachedWrap
         && (Integer)((TypoForm)this.form).animUnit.get() == this.cachedAnimUnit
         && gradient == this.cachedGradient
         && gradientFrom == this.cachedGradientFrom
         && gradientTo == this.cachedGradientTo
         && gradientMode == this.cachedGradientMode) {
         return this.cachedLayout;
      }

      this.cachedGradient = gradient;
      this.cachedGradientFrom = gradientFrom;
      this.cachedGradientTo = gradientTo;
      this.cachedGradientMode = gradientMode;
      TypoFormRenderer.Layout layout = new TypoFormRenderer.Layout();
      TextRenderer renderer = font.getRenderer();
      int lh = lineHeight > 0.0F ? Math.round(lineHeight) : font.getHeight() + 4;
      layout.lineHeight = lh;
      List<List<TypoMarkup.Glyph>> rows = wrap > 0 ? wrapRows(renderer, content, wrap) : TypoMarkup.parse(content);
      if (gradient) {
         applyGradient(rows, gradientFrom, gradientTo, gradientMode);
      }

      int index = 0;
      int word = 0;
      boolean inWord = false;
      boolean words = (Integer)((TypoForm)this.form).animUnit.get() == 1;

      for (List<TypoMarkup.Glyph> row : rows) {
         TypoFormRenderer.Line line = new TypoFormRenderer.Line();
         float x = 0.0F;

         for (TypoMarkup.Glyph styled : row) {
            TypoFormRenderer.Glyph glyph = new TypoFormRenderer.Glyph();
            if (Character.isSpaceChar(styled.codePoint)) {
               inWord = false;
            } else if (!inWord) {
               inWord = true;
               word++;
            }

            glyph.codePoint = styled.codePoint;
            glyph.text = styled.text();
            glyph.rgb = styled.rgb;
            glyph.style = styled.style;
            glyph.prefix = TypoMarkup.drawPrefix(styled.style);
            glyph.x = x;
            glyph.w = renderer.getWidth(glyph.prefix + glyph.text);
            glyph.index = index++;
            glyph.unit = words ? Math.max(0, word - 1) : glyph.index;
            x += glyph.w + tracking;
            line.glyphs.add(glyph);
         }

         for (int i = 0; i < line.glyphs.size(); i++) {
            TypoFormRenderer.Glyph glyph = (TypoFormRenderer.Glyph)line.glyphs.get(i);
            TypoFormRenderer.Glyph next = i + 1 < line.glyphs.size() ? (TypoFormRenderer.Glyph)line.glyphs.get(i + 1) : null;
            glyph.underline = glyph.w + (next != null && next.has(8) ? tracking : 0.0F);
            glyph.strike = glyph.w + (next != null && next.has(4) ? tracking : 0.0F);
         }

         line.width = Math.max(0.0F, x - tracking);
         layout.maxWidth = Math.max(layout.maxWidth, (int)Math.ceil(line.width));
         layout.lines.add(line);
         inWord = false;
      }

      layout.charCount = index;
      layout.unitCount = words ? Math.max(1, word) : Math.max(1, index);
      layout.height = layout.lines.isEmpty() ? font.getHeight() : (layout.lines.size() - 1) * lh + font.getHeight();
      this.cachedLayout = layout;
      this.cachedFont = font;
      this.cachedContent = content;
      this.cachedTracking = tracking;
      this.cachedLineHeight = lineHeight;
      this.cachedWrap = wrap;
      this.cachedAnimUnit = (Integer)((TypoForm)this.form).animUnit.get();
      return layout;
   }

   private static List<List<TypoMarkup.Glyph>> wrapRows(TextRenderer renderer, String content, int width) {
      List<List<TypoMarkup.Glyph>> rows = new ArrayList();

      for (OrderedText text : renderer.wrapLines(StringVisitable.plain(content), width)) {
         List<TypoMarkup.Glyph> row = new ArrayList();
         text.accept((index, style, codePoint) -> {
            if (!Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
               row.add(new TypoMarkup.Glyph(codePoint, rgb(style), styleBits(style)));
            }

            return true;
         });
         rows.add(row);
      }

      return rows;
   }

   private static int rgb(Style style) {
      TextColor color = style.getColor();
      return color == null ? -1 : color.getRgb() & 16777215;
   }

   private static void applyGradient(List<List<TypoMarkup.Glyph>> rows, int from, int to, int mode) {
      for (int r = 0; r < rows.size(); r++) {
         List<TypoMarkup.Glyph> row = (List<TypoMarkup.Glyph>)rows.get(r);

         for (int i = 0; i < row.size(); i++) {
            TypoMarkup.Glyph glyph = (TypoMarkup.Glyph)row.get(i);
            if (glyph.rgb == -1) {
               float position = mode == 1 ? r : i + 0.5F;
               float span = mode == 1 ? Math.max(1, rows.size() - 1) : row.size();
               int rgb = TextGradient.at(from, to, mode, position, span);
               row.set(i, new TypoMarkup.Glyph(glyph.codePoint, rgb, glyph.style));
            }
         }
      }
   }

   private static int styleBits(Style style) {
      return (style.isObfuscated() ? 1 : 0)
         | (style.isBold() ? 2 : 0)
         | (style.isStrikethrough() ? 4 : 0)
         | (style.isUnderlined() ? 8 : 0)
         | (style.isItalic() ? 16 : 0);
   }

   private TypoFormRenderer.GlyphAnim animate(int index, int count, float time) {
      TypoFormRenderer.GlyphAnim anim = new TypoFormRenderer.GlyphAnim();
      int mode = (Integer)((TypoForm)this.form).animMode.get();
      if (mode != 0 && count != 0) {
         float progress = ((TypoForm)this.form).timeRemap.get()
            ? TimeRemap.progress(
               time,
               (Float)((TypoForm)this.form).timeSpeed.get(),
               (Float)((TypoForm)this.form).timeOffset.get(),
               (Float)((TypoForm)this.form).timeDuration.get(),
               (Integer)((TypoForm)this.form).timeMode.get()
            )
            : (Float)((TypoForm)this.form).animProgress.get();
         float stagger = (Float)((TypoForm)this.form).animStagger.get();
         float amount = (Float)((TypoForm)this.form).animAmount.get();
         float order = TextAnimOrder.position(index, count, (Integer)((TypoForm)this.form).animOrder.get(), (Integer)((TypoForm)this.form).animSeed.get());
         float eased;
         if (stagger <= 0.0F) {
            eased = BezierEasing.eval(
               progress,
               (Float)((TypoForm)this.form).easeX1.get(),
               (Float)((TypoForm)this.form).easeY1.get(),
               (Float)((TypoForm)this.form).easeX2.get(),
               (Float)((TypoForm)this.form).easeY2.get()
            );
         } else {
            float span = 1.0F - stagger;
            float start = order * stagger;
            float local = BezierEasing.clamp((progress - start) / Math.max(0.05F, span), 0.0F, 1.0F);
            eased = BezierEasing.eval(
               local,
               (Float)((TypoForm)this.form).easeX1.get(),
               (Float)((TypoForm)this.form).easeY1.get(),
               (Float)((TypoForm)this.form).easeX2.get(),
               (Float)((TypoForm)this.form).easeY2.get()
            );
         }

         switch (mode) {
            case 1:
               anim.alpha = eased >= 0.05F ? 1.0F : 0.0F;
               break;
            case 2:
               anim.alpha = eased;
               break;
            case 3:
               anim.dy = (1.0F - eased) * 16.0F * amount;
               anim.alpha = eased;
               break;
            case 4:
               anim.dy = -(1.0F - eased) * 16.0F * amount;
               anim.alpha = eased;
               break;
            case 5:
               anim.dx = (1.0F - eased) * 24.0F * amount;
               anim.alpha = eased;
               break;
            case 6:
               anim.dx = -(1.0F - eased) * 24.0F * amount;
               anim.alpha = eased;
               break;
            case 7:
               anim.scale = Math.max(0.001F, eased);
               anim.alpha = eased;
               break;
            case 8:
               anim.angle = (1.0F - eased) * 90.0F * amount;
               anim.alpha = eased;
               break;
            case 9:
               anim.scale = eased + (float)Math.sin(eased * Math.PI) * 0.35F * amount;
               anim.alpha = Math.min(1.0F, eased * 2.0F);
               break;
            case 10: {
               float phase = ((TypoForm)this.form).animLoop.get()
                  ? time * 0.15F * (Float)((TypoForm)this.form).animSpeed.get() + index * 0.45F
                  : progress * count * 0.45F - index * 0.45F;
               anim.dy = (float)Math.sin(phase) * 6.0F * amount;
               break;
            }
            case 11: {
               float phase = ((TypoForm)this.form).animLoop.get()
                  ? time * 0.1F * (Float)((TypoForm)this.form).animSpeed.get() + index * 0.4F
                  : progress * count * 0.4F - index * 0.4F;
               anim.dy = -Math.abs((float)Math.sin(phase)) * 8.0F * amount;
               break;
            }
            case 12:
               float pop = (1.0F - eased) * amount;
               anim.scale = 1.0F + pop * 1.8F;
               anim.dy = -pop * 5.0F;
               anim.alpha = Math.min(1.0F, eased * 2.5F);
               if (eased > 0.02F && eased < 0.999F) {
                  anim.alpha = anim.alpha * (0.65F + 0.35F * (float)Math.sin(time * 1.7F + index));
               }
         }

         return anim;
      } else {
         return anim;
      }
   }

   private void applyBillboard(FormRenderingContext context) {
      Matrix4f modelMatrix = context.stack.peek().getPositionMatrix();
      if ((Boolean)((TypoForm)this.form).billboardYaw.get()) {
         Vector3f translation = modelMatrix.getTranslation(new Vector3f());
         Vector3f scale = modelMatrix.getScale(new Vector3f());
         Matrix4f view = context.camera.view;
         modelMatrix.identity().translate(translation).mul(view).rotateY(BillboardMath.yawTowardCamera(view, translation)).scale(scale);
      } else {
         Vector3f scale = Vectors.TEMP_3F;
         modelMatrix.getScale(scale);
         modelMatrix.m00(1.0F).m01(0.0F).m02(0.0F);
         modelMatrix.m10(0.0F).m11(1.0F).m12(0.0F);
         modelMatrix.m20(0.0F).m21(0.0F).m22(1.0F);
         modelMatrix.scale(scale);
      }

      context.stack.peek().getNormalMatrix().identity();
   }

   private Color fillColor(Color base, TypoFormRenderer.Glyph glyph, float alpha) {
      Color fill = base.copy();
      if (glyph.rgb != -1) {
         fill.r = (glyph.rgb >> 16 & 0xFF) / 255.0F;
         fill.g = (glyph.rgb >> 8 & 0xFF) / 255.0F;
         fill.b = (glyph.rgb & 0xFF) / 255.0F;
      }

      fill.a *= alpha;
      return fill;
   }

   private static Color followColor(Color configured, TypoFormRenderer.Glyph glyph, boolean follow, float shade) {
      Color color = configured.copy();
      if (follow && glyph.rgb != -1) {
         int rgb = ChatColorCodes.shade(glyph.rgb, shade);
         color.r = (rgb >> 16 & 0xFF) / 255.0F;
         color.g = (rgb >> 8 & 0xFF) / 255.0F;
         color.b = (rgb & 0xFF) / 255.0F;
      }

      return color;
   }

   private static String piece(TypoFormRenderer.Glyph glyph, int frame) {
      if (!glyph.has(1)) {
         return glyph.prefix + glyph.text;
      }

      int codePoint = TypoMarkup.obfuscate(glyph.codePoint, frame * 31 + glyph.index);
      return glyph.prefix + new String(Character.toChars(codePoint));
   }

   private static float pieceShift(TextRenderer renderer, TypoFormRenderer.Glyph glyph, String piece) {
      return glyph.has(1) ? (glyph.w - renderer.getWidth(piece)) / 2.0F : 0.0F;
   }

   private boolean renderTextModel(FormRenderingContext context) {
      if ((Integer)((TypoForm)this.form).animMode.get() != 0) {
         return this.renderAnimatedTextModel(context);
      }

      this.clearTextGlyphModels();
      context.stack.push();

      try {
         String key = TextModel3D.key((TypoForm)this.form);
         if (!key.equals(this.textModelKey)) {
            if (this.textModel != null) {
               this.textModel.delete();
            }

            this.textModel = null;
            this.textModelKey = key;

            try {
               this.textModel = TextModel3D.build((TypoForm)this.form);
            } catch (Throwable e) {
               e.printStackTrace();
            }
         }

         if (this.textModel == null) {
            return false;
         }

         if ((Boolean)((TypoForm)this.form).billboard.get()) {
            this.applyBillboard(context);
         }

         float scale = this.textModel.scale();
         context.stack.scale(scale, scale, scale);
         context.stack.translate((1.0F - ((Integer)((TypoForm)this.form).align.get()).intValue()) * 0.5F * this.textModel.contentFraction(), 0.0F, 0.0F);
         Color color = new Color().set(context.color, true);
         FormColorBlend.blend(color, (Color)((TypoForm)this.form).color.get());
         OverlayBlend.apply(color, (Color)((TypoForm)this.form).overlayColor.get());
         int light = context.light;
         boolean glowing = (Boolean)((TypoForm)this.form).glow.get() && !context.isPicking() && (Float)((TypoForm)this.form).glowStrength.get() > 0.0F;
         float hdr = 1.0F;
         if (glowing) {
            float amount = Math.min(1.0F, (Float)((TypoForm)this.form).glowStrength.get());
            int u = light & 65535;
            int v = light >> 16 & 65535;
            u = (int)(u + (240 - u) * amount);
            light = u | v << 16;
         }

         BBSModClient.getTextures().bindTexture(this.textModel.texture);
         GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
         gameRenderer.getLightmapTextureManager().enable();
         gameRenderer.getOverlayTexture().setupOverlayColor();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         ShaderProgram shader;
         if (context.isPicking()) {
            this.setupTarget(context, BBSShaders.getPickerModelsProgram());
            shader = BBSShaders.getPickerModelsProgram();
         } else if (BBSRendering.isIrisShadersEnabled()) {
            shader = GameRenderer.getRenderTypeEntityTranslucentProgram();
         } else {
            shader = BBSShaders.getModel();
         }

         if (glowing) {
            RenderSystem.setShaderColor(hdr, hdr, hdr, 1.0F);
         }

         if (!context.isPicking() && this.textModel.glowModel != null) {
            BBSModClient.getTextures().bindTexture(this.textModel.glowTexture);
            context.stack.push();
            context.stack.translate(0.0F, 0.0F, -this.textModel.depth - 0.02F);
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(SrcFactor.SRC_ALPHA, DstFactor.ONE);
            ModelVAORenderer.render(shader, this.textModel.glowModel, context.stack, 1.0F, 1.0F, 1.0F, 1.0F, 15728880, OverlayTexture.DEFAULT_UV);
            context.stack.pop();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(true);
            BBSModClient.getTextures().bindTexture(this.textModel.texture);
         }

         ModelVAORenderer.render(shader, this.textModel.model, context.stack, color.r, color.g, color.b, color.a, light, OverlayTexture.DEFAULT_UV);
         if (glowing) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gameRenderer.getOverlayTexture().teardownOverlayColor();
         gameRenderer.getLightmapTextureManager().disable();
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         return true;
      } catch (Throwable e) {
         e.printStackTrace();
         return false;
      } finally {
         context.stack.pop();
      }
   }

   private boolean renderAnimatedTextModel(FormRenderingContext context) {
      context.stack.push();

      try {
         String key = TextModel3D.key((TypoForm)this.form);
         if (!key.equals(this.textGlyphModelsKey)) {
            this.clearTextGlyphModels();
            this.textGlyphModelsKey = key;
         }

         if (this.textModel != null) {
            this.textModel.delete();
            this.textModel = null;
            this.textModelKey = null;
         }

         if ((Boolean)((TypoForm)this.form).billboard.get()) {
            this.applyBillboard(context);
         }

         context.stack.scale(0.0625F, 0.0625F, 0.0625F);
         Color base = new Color().set(context.color, true);
         FormColorBlend.blend(base, (Color)((TypoForm)this.form).color.get());
         OverlayBlend.apply(base, (Color)((TypoForm)this.form).overlayColor.get());
         int light = context.light;
         boolean glowing = (Boolean)((TypoForm)this.form).glow.get() && !context.isPicking() && (Float)((TypoForm)this.form).glowStrength.get() > 0.0F;
         if (glowing) {
            float amount = Math.min(1.0F, (Float)((TypoForm)this.form).glowStrength.get());
            int u = light & 65535;
            int v = light >> 16 & 65535;
            u = (int)(u + (240 - u) * amount);
            light = u | v << 16;
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         GameRenderer gameRenderer = MinecraftClient.getInstance().gameRenderer;
         gameRenderer.getLightmapTextureManager().enable();
         gameRenderer.getOverlayTexture().setupOverlayColor();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         ShaderProgram shader;
         if (context.isPicking()) {
            this.setupTarget(context, BBSShaders.getPickerModelsProgram());
            shader = BBSShaders.getPickerModelsProgram();
         } else if (BBSRendering.isIrisShadersEnabled()) {
            shader = GameRenderer.getRenderTypeEntityTranslucentProgram();
         } else {
            shader = BBSShaders.getModel();
         }

         FontRenderer font = this.getFont(Float.MAX_VALUE);
         TypoFormRenderer.Layout layout = this.layout(font);
         float time = this.clock(context);
         float anchorX = ((Integer)((TypoForm)this.form).align.get()).intValue() / 2.0F;
         float ox = -layout.maxWidth * anchorX;
         float oy = -layout.height / 2.0F;

         for (int l = 0; l < layout.lines.size(); l++) {
            TypoFormRenderer.Line line = (TypoFormRenderer.Line)layout.lines.get(l);
            float lineOffset = (layout.maxWidth - line.width) * anchorX;
            float y = oy + l * layout.lineHeight;

            for (int i = 0; i < line.glyphs.size(); i++) {
               TypoFormRenderer.Glyph glyph = (TypoFormRenderer.Glyph)line.glyphs.get(i);
               if (!Character.isSpaceChar(glyph.codePoint)) {
                  TypoFormRenderer.GlyphAnim anim = this.animate(glyph.unit, layout.unitCount, time);
                  if (!(anim.alpha <= 0.01F)) {
                     TypoFormRenderer.Glyph next = i + 1 < line.glyphs.size() ? (TypoFormRenderer.Glyph)line.glyphs.get(i + 1) : null;
                     TextModel3D model = this.textGlyphModel(glyph, next);
                     if (model != null) {
                        float x = ox + lineOffset + glyph.x;
                        float pivotX = x + glyph.w / 2.0F;
                        float pivotY = y + font.getHeight() / 2.0F;
                        float modelX = x + (model.width / 2.0F - model.padding) / model.supersample;
                        float modelY = y + (model.height / 2.0F - model.padding) / model.supersample;
                        float modelScale = model.scale() * 16.0F;
                        Color color = base.copy();
                        color.a = color.a * anim.alpha;
                        context.stack.push();
                        context.stack.translate(pivotX + anim.dx, -pivotY - anim.dy, 0.0F);
                        if (anim.angle != 0.0F) {
                           context.stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim.angle));
                        }

                        if (anim.scale != 1.0F) {
                           context.stack.scale(anim.scale, anim.scale, 1.0F);
                        }

                        context.stack.translate(modelX - pivotX, -modelY + pivotY, 0.0F);
                        context.stack.scale(modelScale, modelScale, modelScale);
                        BBSModClient.getTextures().bindTexture(model.texture);
                        ModelVAORenderer.render(shader, model.model, context.stack, color.r, color.g, color.b, color.a, light, OverlayTexture.DEFAULT_UV);
                        context.stack.pop();
                     }
                  }
               }
            }
         }

         if (glowing) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         gameRenderer.getOverlayTexture().teardownOverlayColor();
         gameRenderer.getLightmapTextureManager().disable();
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         return true;
      } catch (Throwable e) {
         e.printStackTrace();
         return false;
      } finally {
         context.stack.pop();
      }
   }

   private TextModel3D textGlyphModel(TypoFormRenderer.Glyph glyph, TypoFormRenderer.Glyph following) {
      boolean underline = glyph.has(8) && following != null && following.has(8);
      boolean strike = glyph.has(4) && following != null && following.has(4);
      String key = glyph.codePoint + "|" + glyph.rgb + "|" + glyph.style + "|" + underline + "|" + strike;
      if (!this.textGlyphModels.containsKey(key)) {
         TextModel3D model = null;

         try {
            TypoMarkup.Glyph next = following == null ? null : new TypoMarkup.Glyph(following.codePoint, following.rgb, following.style);
            model = TextModel3D.buildGlyph((TypoForm)this.form, new TypoMarkup.Glyph(glyph.codePoint, glyph.rgb, glyph.style), next);
         } catch (Throwable e) {
            e.printStackTrace();
         }

         this.textGlyphModels.put(key, model);
      }

      return (TextModel3D)this.textGlyphModels.get(key);
   }

   private void clearTextGlyphModels() {
      for (TextModel3D model : this.textGlyphModels.values()) {
         if (model != null) {
            model.delete();
         }
      }

      this.textGlyphModels.clear();
      this.textGlyphModelsKey = null;
   }

   public void render3D(FormRenderingContext context) {
      if (!(Boolean)((TypoForm)this.form).model3D.get() || !this.renderTextModel(context)) {
         context.stack.push();
         if ((Boolean)((TypoForm)this.form).billboard.get()) {
            this.applyBillboard(context);
         }

         FontRenderer font = this.getFont(Float.MAX_VALUE);
         CustomVertexConsumerProvider consumers = FormUtilsClient.getProvider();
         TextRenderer renderer = font.getRenderer();
         TypoFormRenderer.Layout layout = this.layout(font);
         if (!(Boolean)((TypoForm)this.form).billboard.get()) {
            Vector3f normal = context.stack.peek().getPositionMatrix().transformDirection(new Vector3f(0.0F, 0.0F, 1.0F));
            float length = normal.length();
            if (length > 1.0E-5F) {
               normal.div(length);
               if (normal.z < 0.0F) {
                  context.stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                  normal.x = -normal.x;
                  normal.z = -normal.z;
               }

               float yaw = (float)Math.atan2(normal.x, normal.z);
               float limit = (float)Math.acos(0.34F);
               float clamped = Math.max(-limit, Math.min(limit, yaw));
               if (Math.abs(clamped - yaw) > 1.0E-4F) {
                  context.stack.multiply(RotationAxis.POSITIVE_Y.rotation(clamped - yaw));
               }
            }
         }

         float scale = 0.0625F;
         int light = context.light;
         MatrixStackUtils.scaleStack(context.stack, scale, -scale, scale);
         RenderSystem.disableCull();
         if (context.isPicking()) {
            CustomVertexConsumerProvider.hijackVertexFormat(layer -> {
               this.setupTarget(context, BBSShaders.getPickerModelsProgram());
               RenderSystem.setShader(BBSShaders::getPickerModelsProgram);
            });
            light = 0;
         }

         boolean glowing = (Boolean)((TypoForm)this.form).glow.get() && !context.isPicking() && (Float)((TypoForm)this.form).glowStrength.get() > 0.0F;
         float hdr = 1.0F;
         if (glowing) {
            float amount = Math.min(1.0F, (Float)((TypoForm)this.form).glowStrength.get());
            int u = light & 65535;
            int v = light >> 16 & 65535;
            u = (int)(u + (240 - u) * amount);
            light = u | v << 16;
            hdr = 1.0F + Math.min(4.0F, (Float)((TypoForm)this.form).glowStrength.get()) * 0.9F;
         }

         float time = this.clock(context);
         int frame = (int)Math.floor(time);
         this.decorating = !context.isPicking();
         this.decorationBaseline = baseline(font);
         this.decorationThickness = Math.max(1.0F, font.getHeight() / 7.0F);
         this.decorations.clear();
         float anchorX = ((Integer)((TypoForm)this.form).align.get()).intValue() / 2.0F;
         int ox = (int)(-layout.maxWidth * anchorX);
         int oy = -layout.height / 2;
         Color base = new Color().set(context.color, true);
         FormColorBlend.blend(base, (Color)((TypoForm)this.form).color.get());
         OverlayBlend.apply(base, (Color)((TypoForm)this.form).overlayColor.get());
         Color shadowColor = ((Color)((TypoForm)this.form).shadowColor.get()).copy();
         shadowColor.mul(context.color);
         boolean outlined = ((Color)((TypoForm)this.form).outlineColor.get()).a > 0.0F && (Float)((TypoForm)this.form).outlineWidth.get() > 0.0F;
         this.renderPlate(context, ox, oy, layout);

         for (int l = 0; l < layout.lines.size(); l++) {
            TypoFormRenderer.Line line = (TypoFormRenderer.Line)layout.lines.get(l);
            int lineOffset = (int)((layout.maxWidth - line.width) * anchorX);
            int y = oy + l * layout.lineHeight;

            for (TypoFormRenderer.Glyph glyph : line.glyphs) {
               TypoFormRenderer.GlyphAnim anim = this.animate(glyph.unit, layout.unitCount, time);
               if (!(anim.alpha <= 0.01F)) {
                  float x = ox + lineOffset + glyph.x;
                  context.stack.push();
                  context.stack.translate(x + glyph.w / 2.0F, y + font.getHeight() / 2.0F, 0.0F);
                  context.stack.translate(anim.dx, anim.dy, 0.0F);
                  if (anim.angle != 0.0F) {
                     context.stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(anim.angle));
                  }

                  if (anim.scale != 1.0F) {
                     context.stack.scale(anim.scale, anim.scale, 1.0F);
                  }

                  context.stack.translate(-glyph.w / 2.0F, -font.getHeight() / 2.0F, 0.0F);
                  String piece = piece(glyph, frame);
                  float shift = pieceShift(renderer, glyph, piece);
                  Color fill = this.fillColor(base, glyph, anim.alpha);
                  if ((Float)((TypoForm)this.form).depth.get() > 0.0F) {
                     this.drawDepth(renderer, consumers, context.stack, glyph, piece, shift, fill, light);
                  }

                  if ((Boolean)((TypoForm)this.form).glow.get()) {
                     this.drawGlow(renderer, consumers, context.stack, glyph, piece, shift, font.getHeight(), anim.alpha, light);
                  }

                  if (shadowColor.a > 0.0F) {
                     Color shadow = followColor((Color)((TypoForm)this.form).shadowColor.get(), glyph, (Boolean)((TypoForm)this.form).shadowFollow.get(), 0.25F);
                     shadow.mul(context.color);
                     shadow.a = shadow.a * anim.alpha;
                     context.stack.push();
                     context.stack.translate(0.0F, 0.0F, -0.1F);
                     this.drawPiece(
                        renderer,
                        consumers,
                        context.stack.peek().getPositionMatrix(),
                        glyph,
                        piece,
                        shift,
                        (Float)((TypoForm)this.form).shadowX.get(),
                        (Float)((TypoForm)this.form).shadowY.get(),
                        shadow.getARGBColor(),
                        light,
                        false
                     );
                     context.stack.pop();
                  }

                  if (outlined) {
                     Color outline = followColor(
                        (Color)((TypoForm)this.form).outlineColor.get(), glyph, (Boolean)((TypoForm)this.form).outlineFollow.get(), 0.4F
                     );
                     outline.a = outline.a * anim.alpha;
                     this.drawOutline(renderer, consumers, context.stack, glyph, piece, shift, outline, light);
                  }

                  this.drawPiece(
                     renderer, consumers, context.stack.peek().getPositionMatrix(), glyph, piece, shift, 0.0F, 0.0F, fill.getARGBColor(), light, true
                  );
                  context.stack.pop();
               }
            }
         }

         RenderSystem.enableDepthTest();
         RenderSystem.enableCull();
         if (glowing) {
            RenderSystem.setShaderColor(hdr, hdr, hdr, 1.0F);
         }

         this.flushDecorations(consumers);
         consumers.draw();
         if (glowing) {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         }

         CustomVertexConsumerProvider.clearRunnables();
         context.stack.pop();
      }
   }

   private void drawPiece(
      TextRenderer renderer,
      CustomVertexConsumerProvider consumers,
      Matrix4f matrix,
      TypoFormRenderer.Glyph glyph,
      String piece,
      float shift,
      float x,
      float y,
      int argb,
      int light,
      boolean front
   ) {
      renderer.draw(piece, x + shift, y, argb, false, matrix, consumers, TextLayerType.NORMAL, 0, light);
      if (this.decorating) {
         float z = front ? 0.01F : -0.01F;
         float thickness = this.decorationThickness;
         if (glyph.has(8)) {
            float top = y + this.decorationBaseline + thickness;
            this.decorate(matrix, x, top, x + glyph.underline, top + thickness, z, argb, light);
         }

         if (glyph.has(4)) {
            float middle = y + this.decorationBaseline * 0.57F;
            this.decorate(matrix, x, middle - thickness / 2.0F, x + glyph.strike, middle + thickness / 2.0F, z, argb, light);
         }
      }
   }

   private void decorate(Matrix4f matrix, float x1, float y1, float x2, float y2, float z, int argb, int light) {
      TypoFormRenderer.Decoration decoration = new TypoFormRenderer.Decoration(argb, light);
      matrix.transformPosition(x1, y2, z, decoration.corners[0]);
      matrix.transformPosition(x2, y2, z, decoration.corners[1]);
      matrix.transformPosition(x2, y1, z, decoration.corners[2]);
      matrix.transformPosition(x1, y1, z, decoration.corners[3]);
      this.decorations.add(decoration);
   }

   private void flushDecorations(CustomVertexConsumerProvider consumers) {
      if (!this.decorations.isEmpty()) {
         VertexConsumer buffer = consumers.getBuffer(RenderLayer.getTextBackground());

         for (TypoFormRenderer.Decoration decoration : this.decorations) {
            for (Vector3f corner : decoration.corners) {
               buffer.vertex(corner.x, corner.y, corner.z).color(decoration.color).light(decoration.light).next();
            }
         }

         this.decorations.clear();
      }
   }

   private void drawOutline(
      TextRenderer renderer,
      CustomVertexConsumerProvider consumers,
      MatrixStack stack,
      TypoFormRenderer.Glyph glyph,
      String piece,
      float shift,
      Color outline,
      int light
   ) {
      float w = (Float)((TypoForm)this.form).outlineWidth.get();
      Matrix4f matrix = stack.peek().getPositionMatrix();
      int argb = outline.getARGBColor();
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, w, 0.0F, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, -w, 0.0F, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, 0.0F, w, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, 0.0F, -w, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, w, w, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, -w, w, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, w, -w, argb, light, false);
      this.drawPiece(renderer, consumers, matrix, glyph, piece, shift, -w, -w, argb, light, false);
   }

   private void drawDepth(
      TextRenderer renderer,
      CustomVertexConsumerProvider consumers,
      MatrixStack stack,
      TypoFormRenderer.Glyph glyph,
      String piece,
      float shift,
      Color fill,
      int light
   ) {
      float depth = (Float)((TypoForm)this.form).depth.get();
      int steps = Math.max(1, Math.min(32, (Integer)((TypoForm)this.form).depthSteps.get()));
      float step = depth / steps;
      Color configured = (Color)((TypoForm)this.form).depthColor.get();
      boolean auto = configured.a <= 0.0F;

      for (int i = steps; i >= 1; i--) {
         float f = (float)i / steps;
         float shade = 0.55F + 0.45F * (1.0F - f);
         Color side = new Color(
            auto ? fill.r * 0.5F : configured.r * shade,
            auto ? fill.g * 0.5F : configured.g * shade,
            auto ? fill.b * 0.5F : configured.b * shade,
            fill.a * (auto ? 0.9F : configured.a)
         );
         stack.push();
         stack.translate(0.0F, 0.0F, -step * i);
         this.drawPiece(renderer, consumers, stack.peek().getPositionMatrix(), glyph, piece, shift, 0.0F, 0.0F, side.getARGBColor(), light, false);
         stack.pop();
      }
   }

   private void drawGlow(
      TextRenderer renderer,
      CustomVertexConsumerProvider consumers,
      MatrixStack stack,
      TypoFormRenderer.Glyph glyph,
      String piece,
      float shift,
      float glyphHeight,
      float animAlpha,
      int light
   ) {
      Color glow = (Color)((TypoForm)this.form).glowColor.get();
      float alpha = glow.a * (Float)((TypoForm)this.form).glowStrength.get() * animAlpha * 0.85F;
      if (!(alpha <= 0.0F)) {
         float scale = 1.0F + 0.03F * Math.max(0.0F, (Float)((TypoForm)this.form).glowRadius.get());
         Color color = new Color(glow.r, glow.g, glow.b, alpha);
         int argb = color.getARGBColor();
         stack.push();
         stack.translate(glyph.w / 2.0F, glyphHeight / 2.0F, -0.06F);
         stack.scale(scale, scale, 1.0F);
         stack.translate(-glyph.w / 2.0F, -glyphHeight / 2.0F, 0.0F);
         this.drawPiece(renderer, consumers, stack.peek().getPositionMatrix(), glyph, piece, shift, 0.0F, 0.0F, argb, light, false);
         stack.pop();
      }
   }

   private void renderPlate(FormRenderingContext context, int x, int y, TypoFormRenderer.Layout layout) {
      Color color = ((Color)((TypoForm)this.form).background.get()).copy();
      color.mul(context.color);
      if (!(color.a <= 0.0F)) {
         float pad = (Float)((TypoForm)this.form).padding.get();
         BufferBuilder builder = Tessellator.getInstance().getBuffer();
         builder.begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
         Matrix4f matrix = context.stack.peek().getPositionMatrix();
         float x1 = x - pad;
         float y1 = y - pad;
         float x2 = x + layout.maxWidth + pad;
         float y2 = y + layout.height + pad;
         builder.vertex(matrix, x2, y1, -0.2F).color(color.r, color.g, color.b, color.a).next();
         builder.vertex(matrix, x1, y1, -0.2F).color(color.r, color.g, color.b, color.a).next();
         builder.vertex(matrix, x1, y2, -0.2F).color(color.r, color.g, color.b, color.a).next();
         builder.vertex(matrix, x2, y1, -0.2F).color(color.r, color.g, color.b, color.a).next();
         builder.vertex(matrix, x1, y2, -0.2F).color(color.r, color.g, color.b, color.a).next();
         builder.vertex(matrix, x2, y2, -0.2F).color(color.r, color.g, color.b, color.a).next();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.setShader(GameRenderer::getPositionColorProgram);
         BufferRenderer.drawWithGlobalProgram(builder.end());
      }
   }

   public void renderInUI(UIContext context, int x1, int y1, int x2, int y2) {
      FontRenderer font = this.getFont((float)MinecraftClient.getInstance().getWindow().getScaleFactor());
      FontRenderer previous = context.batcher.setFont(font);
      TypoFormRenderer.Layout layout = this.layout(font);

      try {
         float time = (float)(System.currentTimeMillis() % 100000L) / 50.0F;
         int frame = (int)Math.floor(time);
         float anchorX = ((Integer)((TypoForm)this.form).align.get()).intValue() / 2.0F;
         int totalW = x2 - x1 - 8;
         int startY = (y1 + y2) / 2 - layout.height / 2;
         float baseline = baseline(font);
         float thickness = Math.max(1.0F, Math.round(font.getHeight() / 7.0F));

         for (int l = 0; l < layout.lines.size(); l++) {
            TypoFormRenderer.Line line = (TypoFormRenderer.Line)layout.lines.get(l);
            int lineW = (int)line.width;
            int lx = x1 + 4 + (int)((totalW - lineW) * anchorX);
            int y = startY + l * layout.lineHeight;

            for (TypoFormRenderer.Glyph glyph : line.glyphs) {
               TypoFormRenderer.GlyphAnim anim = this.animate(glyph.unit, layout.unitCount, time);
               int gx = lx + (int)glyph.x + (int)anim.dx;
               int gy = y + (int)anim.dy;
               if (!(anim.alpha <= 0.01F)) {
                  int argb = this.fillColor((Color)((TypoForm)this.form).color.get(), glyph, anim.alpha).getARGBColor();
                  String piece = piece(glyph, frame);
                  context.batcher.textShadow(piece, gx + pieceShift(font.getRenderer(), glyph, piece), gy, argb);
                  if (glyph.has(8)) {
                     float top = gy + baseline + thickness;
                     context.batcher.box(gx, top, gx + glyph.underline, top + thickness, argb);
                  }

                  if (glyph.has(4)) {
                     float middle = gy + baseline * 0.57F;
                     context.batcher.box(gx, middle - thickness / 2.0F, gx + glyph.strike, middle + thickness / 2.0F, argb);
                  }
               }
            }
         }
      } finally {
         context.batcher.setFont(previous);
      }
   }

   @Environment(EnvType.CLIENT)
   private static class Decoration {
      final Vector3f[] corners = new Vector3f[]{new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
      final int color;
      final int light;

      Decoration(int color, int light) {
         this.color = color;
         this.light = light;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class Glyph {
      int codePoint;
      String text;
      float x;
      float w;
      int index;
      int unit;
      int rgb = -1;
      int style;
      String prefix = "";
      float underline;
      float strike;

      boolean has(int bit) {
         return (this.style & bit) != 0;
      }
   }

   @Environment(EnvType.CLIENT)
   private static class GlyphAnim {
      float dx;
      float dy;
      float scale = 1.0F;
      float angle;
      float alpha = 1.0F;
   }

   @Environment(EnvType.CLIENT)
   private static class Layout {
      final List<TypoFormRenderer.Line> lines = new ArrayList();
      int maxWidth;
      int height;
      int lineHeight;
      int charCount;
      int unitCount;
   }

   @Environment(EnvType.CLIENT)
   private static class Line {
      final List<TypoFormRenderer.Glyph> glyphs = new ArrayList();
      float width;
   }
}
