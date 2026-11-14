package bbsfx.forms;

import bbsfx.LangKeys;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class ImageForm extends Form {
   public static final Icon ICON = Icons.IMAGE;
   public final ValueLink texture = new ValueLink("texture", null);
   public final ValueFloat width = new ValueFloat("width", 2.0F, 0.25F, 64.0F);
   public final ValueFloat aspect = new ValueFloat("aspect", 0.0F, 0.0F, 8.0F);
   public final ValueFloat opacity = new ValueFloat("opacity", 1.0F, 0.0F, 1.0F);
   public final ValueBoolean billboard = new ValueBoolean("billboard", true);
   public final ValueBoolean throughWalls = new ValueBoolean("throughWalls", false);

   public ImageForm() {
      this.add(this.texture);
      this.add(this.width);
      this.add(this.aspect);
      this.add(this.opacity);
      this.add(this.billboard);
      this.add(this.throughWalls);
   }

   public Icon getIcon() {
      return ICON;
   }

   public String getDefaultDisplayName() {
      return LangKeys.key("bbsfx.form.image").get();
   }
}
