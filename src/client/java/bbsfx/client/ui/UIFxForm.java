package bbsfx.client.ui;

import bbsfx.forms.FxForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UIFxForm extends UIForm<FxForm> {
   public UIFxForm() {
      this.defaultPanel = new UIFxFormPanel(this);
      this.registerPanel(this.defaultPanel, BBSFxKeys.FX_TITLE, FxForm.ICON);
      this.registerDefaultPanels();
   }
}
