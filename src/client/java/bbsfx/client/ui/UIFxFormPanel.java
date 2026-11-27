package bbsfx.client.ui;

import bbsfx.forms.FxForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.values.UIValues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UIFxFormPanel extends UIFormPanel<FxForm> {
   public UIColor primary;
   public UIColor secondary;

   public UIFxFormPanel(UIForm editor) {
      super(editor);
      UICirculate effect = new UICirculate(null);

      for (IKey key : FxForm.EFFECT_LABELS) {
         effect.addLabel(key);
      }

      effect.callback = b -> ((FxForm)this.form).effect.set(effect.getValue());
      effect.valueBinding(() -> effect.setValue((Integer)((FxForm)this.form).effect.get()));
      effect.w(90);
      this.primary = UIValues.color(() -> ((FxForm)this.form).primary).withAlpha();
      this.secondary = UIValues.color(() -> ((FxForm)this.form).secondary).withAlpha();
      UITrackpad size = UIValues.trackpad(() -> ((FxForm)this.form).size);
      UITrackpad progress = UIValues.trackpad(() -> ((FxForm)this.form).progress);
      UITrackpad speed = UIValues.trackpad(() -> ((FxForm)this.form).speed);
      UITrackpad intensity = UIValues.trackpad(() -> ((FxForm)this.form).intensity);
      UITrackpad count = UIValues.trackpad(() -> ((FxForm)this.form).count);
      UITrackpad thickness = UIValues.trackpad(() -> ((FxForm)this.form).thickness);
      UITrackpad layers = UIValues.trackpad(() -> ((FxForm)this.form).layers);
      UITrackpad height = UIValues.trackpad(() -> ((FxForm)this.form).height);
      UITrackpad flicker = UIValues.trackpad(() -> ((FxForm)this.form).flicker);
      UITrackpad colorMix = UIValues.trackpad(() -> ((FxForm)this.form).colorMix);
      UITrackpad fade = UIValues.trackpad(() -> ((FxForm)this.form).fade);
      UITrackpad spin = UIValues.trackpad(() -> ((FxForm)this.form).spin);
      size.values(0.1F, 0.05F, 0.5).tooltip(BBSFxKeys.FX_SIZE);
      progress.values(0.01F).tooltip(BBSFxKeys.FX_PROGRESS);
      speed.values(0.05F).tooltip(BBSFxKeys.FX_SPEED);
      intensity.values(0.1F, 0.05F, 0.5).tooltip(BBSFxKeys.FX_INTENSITY);
      count.values(1.0, 0.5, 4.0).tooltip(BBSFxKeys.FX_COUNT);
      thickness.values(0.01F).tooltip(BBSFxKeys.FX_THICKNESS);
      layers.values(1.0).tooltip(BBSFxKeys.FX_LAYERS);
      height.values(0.05F).tooltip(BBSFxKeys.FX_HEIGHT);
      flicker.values(0.1F).tooltip(BBSFxKeys.FX_FLICKER);
      colorMix.values(0.02F).tooltip(BBSFxKeys.FX_COLOR_MIX);
      fade.values(0.02F).tooltip(BBSFxKeys.FX_FADE);
      spin.values(0.02F).tooltip(BBSFxKeys.FX_SPIN);
      this.options
         .add(
            new IUIElement[]{
               UI.labelRow(BBSFxKeys.FX_EFFECT, effect),
               UI.labelRow(BBSFxKeys.FX_PRIMARY, this.primary),
               UI.labelRow(BBSFxKeys.FX_SECONDARY, this.secondary),
               UI.row(new UIElement[]{size, intensity})
            }
         );
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.FX_PROGRESS).marginTop(3),
               progress,
               UI.row(new UIElement[]{speed, UIValues.toggle(BBSFxKeys.FX_LOOP, () -> ((FxForm)this.form).loop)})
            }
         );
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.FX_COUNT).marginTop(3),
               UI.row(new UIElement[]{count, thickness}),
               UI.row(new UIElement[]{layers, height}),
               UI.row(new UIElement[]{flicker, spin}),
               UI.row(new UIElement[]{colorMix, fade})
            }
         );
      this.options
         .add(
            new IUIElement[]{
               UIValues.toggle(BBSFxKeys.FX_BILLBOARD, () -> ((FxForm)this.form).billboard).marginTop(3),
               UIValues.toggle(BBSFxKeys.FX_ADDITIVE, () -> ((FxForm)this.form).additive)
            }
         );
   }

   public void finishEdit() {
      super.finishEdit();
      this.primary.picker.removeFromParent();
      this.secondary.picker.removeFromParent();
   }
}
