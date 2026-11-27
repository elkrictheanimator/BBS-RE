package bbsfx.client.ui;

import bbsfx.client.ui.skin.UISkin;
import bbsfx.ui.SliderMath;
import java.util.function.Consumer;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.UINumericInput;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UISliderTrackpad extends UITrackpad {
   private static final int INPUT_HEIGHT = 20;
   private static final int HEIGHT = 34;
   private static final int PADDING = 4;
   private IKey label;
   private int numericWidth = 24;
   private boolean sliderDragging;
   private double sliderStartValue;
   private int sliderStartX;

   public UISliderTrackpad(Consumer<Double> callback, IKey label) {
      super(callback);
      this.label = label;
      this.h(34);
   }

   public UISliderTrackpad forcedLabel(IKey label) {
      this.label = label;
      return this;
   }

   public boolean isDragging() {
      return super.isDragging() || this.sliderDragging;
   }

   public boolean subMouseClicked(UIContext context) {
      if (context.mouseButton == 1 && this.sliderDragging) {
         this.setValueAndNotify(this.sliderStartValue);
         this.stopSliderDrag();
         return true;
      }

      if (this.isEnabled() && context.mouseButton == 0 && this.onSlider(context)) {
         if (this.textbox.isFocused()) {
            context.unfocus();
         }

         this.sliderDragging = true;
         this.sliderStartValue = this.getValue();
         this.sliderStartX = context.mouseX;
         this.emitDragStart();
         return true;
      } else if (!this.isEnabled() || !this.textbox.isFocused() && !this.onNumericInput(context)) {
         if (this.isEnabled() && context.mouseButton == 0 && this.area.isInside(context) && context.mouseY < this.area.y + 20) {
            context.focus(this);
            return true;
         } else {
            return super.subMouseClicked(context);
         }
      } else {
         return this.numericMouseClicked(context);
      }
   }

   public boolean subMouseReleased(UIContext context) {
      if (this.sliderDragging) {
         if (context.mouseButton == 1) {
            this.setValueAndNotify(this.sliderStartValue);
         } else if (context.mouseButton == 0) {
            this.updateSlider(context.mouseX);
         }

         this.stopSliderDrag();
         return true;
      } else {
         return !this.textbox.isFocused() && !this.onNumericInput(context) ? super.subMouseReleased(context) : this.numericMouseReleased(context);
      }
   }

   private void stopSliderDrag() {
      this.sliderDragging = false;
      this.emitDragEnd();
   }

   private boolean onSlider(UIContext context) {
      return this.area.isInside(context.mouseX, context.mouseY) && context.mouseY >= this.area.y + 20;
   }

   private boolean onNumericInput(UIContext context) {
      return context.mouseY >= this.area.y && context.mouseY < this.area.y + 20 && context.mouseX >= this.numericX() && context.mouseX < this.area.ex();
   }

   private int numericX() {
      return this.area.ex() - this.numericWidth;
   }

   private void measureNumeric(FontRenderer font) {
      String value = this.integer ? Integer.toString((int)this.getValue()) : UINumericInput.format(this.getValue());
      int desired = Math.max(24, font.getWidth(value) + 4);
      this.numericWidth = Math.min(72, Math.min(desired, Math.max(12, this.area.w / 2)));
   }

   private boolean numericMouseClicked(UIContext context) {
      if (!this.textbox.isFocused()) {
         context.focus(this);
      }

      int[] previous = this.setNumericArea();
      this.textbox.area.copy(this.area);

      try {
         this.textbox.mouseClicked(context.mouseX, context.mouseY, context.mouseButton);
         return true;
      } finally {
         this.restoreArea(previous);
         this.textbox.area.copy(this.area);
      }
   }

   private boolean numericMouseReleased(UIContext context) {
      int[] previous = this.setNumericArea();
      this.textbox.area.copy(this.area);

      try {
         this.textbox.mouseReleased(context.mouseX, context.mouseY, context.mouseButton);
         return true;
      } finally {
         this.restoreArea(previous);
         this.textbox.area.copy(this.area);
      }
   }

   private int[] setNumericArea() {
      int[] previous = new int[]{this.area.x, this.area.y, this.area.w, this.area.h};
      this.area.x = this.textbox.isFocused() ? this.area.x + 3 : this.numericX();
      this.area.w = this.textbox.isFocused() ? this.area.w - 6 : this.area.ex() - this.area.x;
      this.area.h = 20;
      return previous;
   }

   private void restoreArea(int[] previous) {
      this.area.set(previous[0], previous[1], previous[2], previous[3]);
   }

   private void updateSlider(int mouseX) {
      if (Double.isFinite(this.min) && Double.isFinite(this.max) && !(this.max <= this.min)) {
         int dx = mouseX - this.sliderStartX;
         double delta = Math.max(0, Math.abs(dx) - 3) * this.getValueModifier();
         double value = this.sliderStartValue + (dx < 0 ? -delta : delta);
         if (this.integer) {
            value = Math.round(value);
         }

         if (value != this.getValue()) {
            try {
               this.setValueAndNotify(value);
            } catch (Throwable error) {
               this.stopSliderDrag();
            }
         }
      }
   }

   public void render(UIContext context) {
      boolean numericEditing = this.textbox.isFocused() || super.isDragging();
      if (this.sliderDragging) {
         this.updateSlider(context.mouseX);
      } else if (!numericEditing) {
         super.render(context);
      }

      UISkin.ensureLoaded();
      Batcher2D batcher = context.batcher;
      FontRenderer font = batcher.getFont();
      this.measureNumeric(font);
      boolean hovered = this.area.isInside(context) && this.isEnabled();
      int accent = this.isEnabled() ? UISkin.accent : -9936016;
      int outline = !hovered && !this.sliderDragging && !this.textbox.isFocused() ? -11975854 : accent;
      UISkin.roundedBox(batcher, this.area.x, this.area.y, this.area.ex(), this.area.ey(), 5, -534766040);
      UISkin.roundedOutline(batcher, this.area.x, this.area.y, this.area.ex(), this.area.ey(), 5, outline);
      int numericX = this.numericX();
      int labelWidth = Math.max(0, numericX - this.area.x - 4 - 4);
      String label = font.limitToWidth(this.label.get(), labelWidth);
      int textY = this.area.y + (20 - font.getHeight()) / 2;
      batcher.textShadow(label, this.area.x + 4, textY, this.isEnabled() ? -3817262 : -8949122);
      this.renderSlider(context, accent);
      if (!this.sliderDragging && numericEditing) {
         int[] previous = this.setNumericArea();
         this.textbox.area.copy(this.area);

         try {
            super.render(context);
         } finally {
            this.restoreArea(previous);
            this.textbox.area.copy(this.area);
         }
      } else {
         String value = this.integer ? Integer.toString((int)this.getValue()) : UINumericInput.format(this.getValue());
         int valueX = this.area.ex() - 4 - font.getWidth(value);
         batcher.text(value, valueX, textY, this.isEnabled() ? -658689 : -7699310);
         this.renderLockedArea(context);
      }
   }

   private void renderSlider(UIContext context, int accent) {
      Batcher2D batcher = context.batcher;
      int left = this.area.x + 4;
      int right = this.area.ex() - 4;
      int top = this.area.ey() - 8;
      float fraction = (float)SliderMath.fractionOf(this.getValue(), this.min, this.max);
      float knobX = left + fraction * Math.max(0, right - left);
      UISkin.roundedBox(batcher, left, top, right, top + 4, 2, -11976367);
      UISkin.roundedBox(batcher, left, top, knobX, top + 4, 2, accent);
      UISkin.roundedBox(batcher, knobX - 4.0F, top - 3, knobX + 5.0F, top + 7, 4, 1711276032);
      UISkin.roundedBox(batcher, knobX - 4.0F, top - 3, knobX + 4.0F, top + 6, 4, this.isEnabled() ? -1 : -5593936);
   }
}
