package bbsfx.client.ui;

import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class BBSFxUI {
   private BBSFxUI() {
   }

   public static UIBezierCurve easeCurve(ValueFloat x1, ValueFloat y1, ValueFloat x2, ValueFloat y2) {
      return new UIBezierCurve(() -> new float[]{(Float)x1.get(), (Float)y1.get(), (Float)x2.get(), (Float)y2.get()}, v -> {
         x1.set(v[0]);
         y1.set(v[1]);
         x2.set(v[2]);
         y2.set(v[3]);
      });
   }

   public static UITrackpad floatTrackpad(IUIClipsDelegate editor, ValueFloat value) {
      return new UITrackpad(v -> editor.editMultiple(value, f -> f.set(v.floatValue())));
   }
}
