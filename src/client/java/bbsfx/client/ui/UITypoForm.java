package bbsfx.client.ui;

import bbsfx.forms.TypoForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UITypoForm extends UIForm<TypoForm> {
   public UITypoForm() {
      this.defaultPanel = new UITypoFormPanel(this);
      this.registerPanel(this.defaultPanel, BBSFxKeys.TYPO_TITLE, TypoForm.ICON);
      this.registerDefaultPanels();
   }
}
