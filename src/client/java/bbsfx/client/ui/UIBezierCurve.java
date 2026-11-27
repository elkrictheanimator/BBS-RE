package bbsfx.client.ui;

import bbsfx.BezierEasing;
import bbsfx.client.ui.skin.UISkin;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.utils.Area;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UIBezierCurve extends UIElement {
   public static final float[] LINEAR = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
   public static final float[] EASE = new float[]{0.25F, 0.1F, 0.25F, 1.0F};
   public static final float[] EASE_IN = new float[]{0.42F, 0.0F, 1.0F, 1.0F};
   public static final float[] EASE_OUT = new float[]{0.0F, 0.0F, 0.58F, 1.0F};
   public static final float[] EASE_IN_OUT = new float[]{0.42F, 0.0F, 0.58F, 1.0F};
   public static final float[] BACK_OUT = new float[]{0.34F, 1.4F, 0.64F, 1.0F};
   public static final float[] ANTICIPATE = new float[]{0.36F, 0.0F, 0.66F, -0.4F};
   public static final float[] EASE_OUT_IN = new float[]{0.2F, 0.8F, 0.8F, 0.2F};
   private static final float[][] PRESETS = new float[][]{LINEAR, EASE, EASE_IN, EASE_OUT, EASE_IN_OUT, EASE_OUT_IN, BACK_OUT, ANTICIPATE};
   private static final float RANGE_MIN = -0.4F;
   private static final float RANGE_MAX = 1.4F;
   private static final float RANGE = 1.8F;
   private static final int PAD = 10;
   private static final int HANDLE = 4;
   private static final int CHIP_H = 12;
   private static final int CHIP_GAP = 2;
   private static final int CHIPS_PER_ROW = 4;
   private static final int CHIPS_ROWS = 2;
   private static final int CHIPS_H = 26;
   private static final int GRAPH_BOTTOM_GAP = 3;
   private static final int GLFW_LEFT = 263;
   private static final int GLFW_RIGHT = 262;
   private static final int GLFW_UP = 265;
   private static final int GLFW_DOWN = 264;
   private static final int GLFW_LEFT_SHIFT = 340;
   private final Supplier<float[]> getter;
   private final Consumer<float[]> setter;
   private int selected = -1;
   private int hovered = -1;
   private int hoveredChip = -1;
   private boolean dragging;
   private final Area graph = new Area();
   private float[] lastPushed;

   public UIBezierCurve(Supplier<float[]> getter, Consumer<float[]> setter) {
      this.getter = getter;
      this.setter = setter;
      this.h(128);
      this.tooltip(BBSFxKeys.CURVE_TOOLTIP);
   }

   private Area graph() {
      this.graph.set(this.area.x, this.area.y, this.area.w, this.area.h - 26 - 3);
      return this.graph;
   }

   private int chipY() {
      return this.area.ey() - 26;
   }

   private float toX(float t) {
      Area graph = this.graph();
      return graph.x + 10 + t * (graph.w - 20);
   }

   private float toY(float v) {
      Area graph = this.graph();
      float norm = (v - -0.4F) / 1.8F;
      return graph.ey() - 10 - norm * (graph.h - 20);
   }

   private float fromX(float x) {
      return BezierEasing.clamp01((x - this.graph().x - 10.0F) / (this.graph().w - 20));
   }

   private float fromY(float y) {
      Area graph = this.graph();
      float norm = (graph.ey() - 10 - y) / (graph.h - 20);
      return BezierEasing.clamp(norm * 1.8F + -0.4F, -0.4F, 1.4F);
   }

   public boolean subMouseClicked(UIContext context) {
      if (context.mouseButton == 0 && this.area.isInside(context.mouseX, context.mouseY)) {
         if (context.mouseY >= this.chipY()) {
            int chip = this.chipAt(context.mouseX, context.mouseY);
            if (chip >= 0) {
               this.setter.accept(this.safePreset(PRESETS[chip]));
            }

            return true;
         } else {
            float[] value = (float[])this.getter.get();
            this.selected = this.pickHandle(context.mouseX, context.mouseY, value);
            this.dragging = true;
            if (this.selected < 0) {
               this.selected = this.nearestHandle(context.mouseX, context.mouseY, value);
               this.applyFromMouse(context);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   public boolean subMouseReleased(UIContext context) {
      if (this.dragging) {
         this.dragging = false;
         this.lastPushed = null;
         return true;
      } else {
         return super.subMouseReleased(context);
      }
   }

   public boolean subKeyPressed(UIContext context) {
      if (this.selected >= 0 && this.area.isInside(context.mouseX, context.mouseY)) {
         int code = context.getKeyCode();
         float step = context.isHeld(340) ? 0.1F : 0.01F;
         float dx = code == 263 ? -step : (code == 262 ? step : 0.0F);
         float dy = code == 264 ? step : (code == 265 ? -step : 0.0F);
         if (dx == 0.0F && dy == 0.0F) {
            return super.subKeyPressed(context);
         }

         float[] value = (float[])((float[])this.getter.get()).clone();
         if (this.selected == 0) {
            value[0] = BezierEasing.clamp01(value[0] + dx);
            value[1] = BezierEasing.clamp(value[1] + dy, -0.4F, 1.4F);
         } else {
            value[2] = BezierEasing.clamp01(value[2] + dx);
            value[3] = BezierEasing.clamp(value[3] + dy, -0.4F, 1.4F);
         }

         this.setter.accept(value);
         return true;
      } else {
         return super.subKeyPressed(context);
      }
   }

   private float[] safePreset(float[] preset) {
      return (float[])preset.clone();
   }

   private void applyFromMouse(UIContext context) {
      if (this.selected >= 0) {
         float[] value = (float[])((float[])this.getter.get()).clone();
         if (this.selected == 0) {
            value[0] = this.fromX(context.mouseX);
            value[1] = this.fromY(context.mouseY);
         } else {
            value[2] = this.fromX(context.mouseX);
            value[3] = this.fromY(context.mouseY);
         }

         float[] previous = this.lastPushed;
         if (previous == null || previous[0] != value[0] || previous[1] != value[1] || previous[2] != value[2] || previous[3] != value[3]) {
            this.lastPushed = value;
            this.setter.accept(value);
         }
      }
   }

   private int pickHandle(int mouseX, int mouseY, float[] value) {
      float x1 = this.toX(value[0]);
      float y1 = this.toY(value[1]);
      float x2 = this.toX(value[2]);
      float y2 = this.toY(value[3]);
      if (Math.abs(mouseX - x1) <= 7.0F && Math.abs(mouseY - y1) <= 7.0F) {
         return 0;
      } else {
         return Math.abs(mouseX - x2) <= 7.0F && Math.abs(mouseY - y2) <= 7.0F ? 1 : -1;
      }
   }

   private int nearestHandle(int mouseX, int mouseY, float[] value) {
      float d1 = Math.abs(mouseX - this.toX(value[0])) + Math.abs(mouseY - this.toY(value[1]));
      float d2 = Math.abs(mouseX - this.toX(value[2])) + Math.abs(mouseY - this.toY(value[3]));
      return d1 <= d2 ? 0 : 1;
   }

   private int chipAt(int mouseX, int mouseY) {
      Area graph = this.graph();
      int chipW = this.chipWidth(graph);
      int rowH = 14;
      int localY = mouseY - this.chipY();
      int row = localY / rowH;
      if (row >= 0 && row < 2 && localY - row * rowH < 12) {
         for (int col = 0; col < 4; col++) {
            int x = graph.x + 2 + col * (chipW + 2);
            if (mouseX >= x && mouseX < x + chipW) {
               int index = row * 4 + col;
               return index < PRESETS.length ? index : -1;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private int chipWidth(Area graph) {
      return (graph.w - 10) / 4;
   }

   public void render(UIContext context) {
      if (this.dragging) {
         this.applyFromMouse(context);
      }

      super.render(context);
      float[] value = (float[])this.getter.get();
      Area graph = this.graph();
      int chipsY = this.chipY();
      this.hovered = graph.isInside(context.mouseX, context.mouseY) ? this.pickHandle(context.mouseX, context.mouseY, value) : -1;
      this.hoveredChip = context.mouseY >= chipsY && this.area.isInside(context.mouseX, context.mouseY) ? this.chipAt(context.mouseX, context.mouseY) : -1;
      int accent = UISkin.accent;
      UISkin.ensureLoaded();
      int radius = Math.min(UISkin.rounding, 6);
      if (UISkin.glass) {
         int body = UISkin.glassAlpha * 2 / 3 << 24 | accent & 16777215;
         UISkin.roundedBox(context.batcher, graph.x, graph.y, graph.ex(), graph.ey(), radius, body);
         UISkin.roundedBox(context.batcher, graph.x + 2, graph.y + 1, graph.ex() - 2, graph.y + 3, Math.max(1, radius - 1), 788529151);
      } else {
         UISkin.roundedBox(context.batcher, graph.x, graph.y, graph.ex(), graph.ey(), radius, -2013265920 | accent & 16777215);
      }

      UISkin.roundedOutline(context.batcher, graph.x, graph.y, graph.ex(), graph.ey(), radius, this.dragging ? -1 : accent);
      context.batcher.clip(graph, context);
      this.drawGrid(context, graph);
      this.drawCurve(context, value, accent);
      this.drawHandles(context, value, accent);
      this.drawPreviewDot(context, value);
      context.batcher.unclip(context);
      this.drawReadout(context, value);
      this.drawChips(context, value, graph);
   }

   private void drawGrid(UIContext context, Area graph) {
      Batcher2D batcher = context.batcher;
      int left = graph.x + 10;
      int right = graph.ex() - 10;
      int top = graph.y + 10;
      int bottom = graph.ey() - 10;
      float y0 = this.toY(0.0F);
      float y1 = this.toY(1.0F);
      batcher.box(left, y0, right, y0 + 1.0F, 1728053247);
      batcher.box(left, y1, right, y1 + 1.0F, 1728053247);
      batcher.box(left, top, left + 1, bottom, 1157627903);
      batcher.box(right - 1, top, right, bottom, 1157627903);

      for (int i = 1; i < 4; i++) {
         float gx = left + (right - left) * i / 4.0F;
         batcher.box(gx, top, gx + 1.0F, bottom, 587202559);
      }

      int steps = 10;

      for (int i = 0; i < steps; i += 2) {
         float f0 = (float)i / steps;
         float f1 = (float)(i + 1) / steps;
         this.line(batcher, (int)this.toX(f0), (int)this.toY(f0), (int)this.toX(f1), (int)this.toY(f1), 822083583);
      }
   }

   private void drawCurve(UIContext context, float[] value, int accent) {
      Batcher2D batcher = context.batcher;
      Area graph = this.graph();
      int steps = Math.max(32, Math.min(72, (graph.w - 20) / 2));
      int lastX = (int)this.toX(0.0F);
      int lastY = (int)this.toY(0.0F);

      for (int i = 1; i <= steps; i++) {
         float t = (float)i / steps;
         float eased = BezierEasing.eval(t, value[0], value[1], value[2], value[3]);
         int x = (int)this.toX(t);
         int y = (int)this.toY(eased);
         batcher.box(lastX - 1, lastY, x + 1, y + 2, 1711276032);
         lastX = x;
         lastY = y;
      }

      lastX = (int)this.toX(0.0F);
      lastY = (int)this.toY(0.0F);

      for (int i = 1; i <= steps; i++) {
         float t = (float)i / steps;
         float eased = BezierEasing.eval(t, value[0], value[1], value[2], value[3]);
         int x = (int)this.toX(t);
         int y = (int)this.toY(eased);
         batcher.box(lastX - 1, lastY - 1, x + 1, y + 1, (this.dragging ? 176 : 120) << 24 | accent & 16777215);
         batcher.box(lastX, lastY, x + 1, y + 1, -1);
         lastX = x;
         lastY = y;
      }
   }

   private void drawHandles(UIContext context, float[] value, int accent) {
      Batcher2D batcher = context.batcher;
      float ox = this.toX(0.0F);
      float oy = this.toY(0.0F);
      float tx = this.toX(1.0F);
      float ty = this.toY(1.0F);
      float hx1 = this.toX(value[0]);
      float hy1 = this.toY(value[1]);
      float hx2 = this.toX(value[2]);
      float hy2 = this.toY(value[3]);
      this.line(batcher, (int)ox, (int)oy, (int)hx1, (int)hy1, 1358954495);
      this.line(batcher, (int)tx, (int)ty, (int)hx2, (int)hy2, 1358954495);

      for (int i = 0; i < 2; i++) {
         float hx = i == 0 ? hx1 : hx2;
         float hy = i == 0 ? hy1 : hy2;
         boolean active = this.selected == i || this.hovered == i;
         int body = active ? -1 : accent;
         batcher.box(hx - 4.0F, hy - 4.0F, hx + 4.0F, hy + 4.0F, (active ? 255 : 224) << 24 | accent & 16777215);
         batcher.outline(hx - 4.0F, hy - 4.0F, hx + 4.0F, hy + 4.0F, active ? -1 : -1862270977);
         if (active) {
            batcher.outline(hx - 4.0F - 1.0F, hy - 4.0F - 1.0F, hx + 4.0F + 1.0F, hy + 4.0F + 1.0F, body);
         }

         String tag = i == 0 ? "P1" : "P2";
         int tagX = (int)hx + 4 + 2;
         if (tagX + batcher.getFont().getWidth(tag) > this.graph().ex() - 2) {
            tagX = (int)hx - 4 - 2 - batcher.getFont().getWidth(tag);
         }

         batcher.textShadow(tag, tagX, hy - 4.0F, active ? -1 : -1342177280 | accent & 16777215);
      }
   }

   private void drawPreviewDot(UIContext context, float[] value) {
      float phase = (float)(System.currentTimeMillis() % 2400L) / 2400.0F;
      float ping = phase < 0.5F ? phase * 2.0F : 2.0F - phase * 2.0F;
      float eased = BezierEasing.eval(ping, value[0], value[1], value[2], value[3]);
      int x = (int)this.toX(ping);
      int y = (int)this.toY(eased);
      context.batcher.box(x - 3, y - 3, x + 3, y + 3, -1879048192);
      context.batcher.box(x - 2, y - 2, x + 2, y + 2, -10929);
   }

   private void drawReadout(UIContext context, float[] value) {
      Batcher2D batcher = context.batcher;
      if (this.hoveredChip >= 0) {
         batcher.textShadow(BBSFxKeys.EASE_PRESET_NAMES[this.hoveredChip].get(), this.graph().x + 5, this.graph().y + 4, -1);
      } else {
         String p1 = "P1 " + this.fmt(value[0]) + "," + this.fmt(value[1]);
         String p2 = "P2 " + this.fmt(value[2]) + "," + this.fmt(value[3]);
         int x = this.graph().x + 5;
         int y = this.graph().y + 4;
         int bright = -1643777;
         int dim = -6642763;
         batcher.textShadow(p1, x, y, this.selected == 0 ? bright : dim);
         batcher.textShadow(p2, x + batcher.getFont().getWidth(p1) + 8, y, this.selected == 1 ? bright : dim);
      }
   }

   private String fmt(float v) {
      return String.format("%.2f", new Object[]{v});
   }

   private void drawChips(UIContext context, float[] value, Area graph) {
      Batcher2D batcher = context.batcher;
      int count = PRESETS.length;
      int chipW = this.chipWidth(graph);
      int rowH = 14;
      int top = this.chipY();

      for (int i = 0; i < count; i++) {
         float[] preset = PRESETS[i];
         int col = i % 4;
         int row = i / 4;
         int x = graph.x + 2 + col * (chipW + 2);
         int y = top + row * rowH;
         boolean hovered = this.hoveredChip == i;
         boolean active = Math.abs(value[0] - preset[0]) < 0.001F
            && Math.abs(value[1] - preset[1]) < 0.001F
            && Math.abs(value[2] - preset[2]) < 0.001F
            && Math.abs(value[3] - preset[3]) < 0.001F;
         UISkin.roundedBox(batcher, x, y, x + chipW, y + 12, 3, (hovered ? 170 : (active ? 204 : 102)) << 24 | UISkin.accent & 16777215);
         if (active) {
            UISkin.roundedOutline(batcher, x, y, x + chipW, y + 12, 3, -1);
         }

         String label = BBSFxKeys.EASE_PRESET_CHIPS[i].get();
         int textX = x + (chipW - batcher.getFont().getWidth(label)) / 2;
         batcher.textShadow(label, textX, y + 2, active ? -1 : -1643777);
      }
   }

   private void line(Batcher2D batcher, int x1, int y1, int x2, int y2, int color) {
      int dx = Math.abs(x2 - x1);
      int dy = Math.abs(y2 - y1);
      int steps = Math.max(dx, dy);
      if (steps != 0) {
         for (int i = 0; i <= steps; i++) {
            float f = (float)i / steps;
            int x = (int)(x1 + (x2 - x1) * f);
            int y = (int)(y1 + (y2 - y1) * f);
            batcher.box(x, y, x + 1, y + 1, color);
         }
      }
   }
}
