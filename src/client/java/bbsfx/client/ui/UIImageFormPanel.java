package bbsfx.client.ui;

import bbsfx.forms.ImageForm;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UITexturePicker;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.values.UIValues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UIImageFormPanel extends UIFormPanel<ImageForm> {
   public UIImageFormPanel(UIForm editor) {
      super(editor);
      UIButton pick = new UIButton(
         BBSFxKeys.IMG_PICK,
         b -> UITexturePicker.open(this.getContext(), (Link)((ImageForm)this.form).texture.get(), link -> ((ImageForm)this.form).texture.set(link))
      );
      pick.tooltip(BBSFxKeys.IMG_PICK);
      UITrackpad width = UIValues.trackpad(() -> ((ImageForm)this.form).width);
      width.limit(0.25, 64.0).tooltip(BBSFxKeys.IMG_WIDTH);
      UITrackpad aspect = UIValues.trackpad(() -> ((ImageForm)this.form).aspect);
      aspect.limit(0.0, 8.0).tooltip(BBSFxKeys.IMG_ASPECT);
      UITrackpad opacity = UIValues.trackpad(() -> ((ImageForm)this.form).opacity);
      opacity.limit(0.0, 1.0).tooltip(BBSFxKeys.IMG_OPACITY);
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.IMG_SECTION),
               pick,
               UI.row(new UIElement[]{width, aspect}),
               opacity,
               UIValues.toggle(BBSFxKeys.IMG_BILLBOARD, () -> ((ImageForm)this.form).billboard),
               UIValues.toggle(BBSFxKeys.IMG_THROUGH, () -> ((ImageForm)this.form).throughWalls),
               BBSFxKeys.hint(BBSFxKeys.IMG_HINT)
            }
         );
   }
}
