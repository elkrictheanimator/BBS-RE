package bbsfx.client.ui;

import bbsfx.forms.ImageForm;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UIImageForm extends UIForm<ImageForm> {
   public UIImageForm() {
      this.defaultPanel = new UIImageFormPanel(this);
      this.registerPanel(this.defaultPanel, BBSFxKeys.IMG_TITLE, ImageForm.ICON);
      this.registerDefaultPanels();
   }
}
