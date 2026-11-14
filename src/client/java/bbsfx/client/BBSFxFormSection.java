package bbsfx.client;

import bbsfx.client.ui.BBSFxKeys;
import bbsfx.forms.FxForm;
import bbsfx.forms.ImageForm;
import bbsfx.forms.TypoForm;
import java.util.Collections;
import java.util.List;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.categories.FormCategory;
import mchorse.bbs_mod.forms.sections.FormSection;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class BBSFxFormSection extends FormSection {
   private FormCategory fx;

   public BBSFxFormSection(FormCategories parent) {
      super(parent);
   }

   public void initiate() {
      this.fx = new FormCategory(BBSFxKeys.SECTION, this.parent.preferences.visible("bbsfx")).icon(Icons.SHAPES);
      this.fx.addForm(new TypoForm());
      this.fx.addForm(world3D());
      this.fx.addForm(kinetic());
      this.fx.addForm(subtitleCard());
      this.fx.addForm(new FxForm());
      this.fx.addForm(new ImageForm());
   }

   private static TypoForm world3D() {
      TypoForm form = new TypoForm();
      form.text.set(BBSFxKeys.PRESET_WORLD3D.get());
      form.model3D.set(true);
      form.depth.set(4.0F);
      form.billboard.set(false);
      return form;
   }

   private static TypoForm kinetic() {
      TypoForm form = new TypoForm();
      form.text.set(BBSFxKeys.PRESET_KINETIC.get());
      form.uppercase.set(true);
      form.outlineColor.set(new Color(0.0F, 0.0F, 0.0F, 1.0F));
      form.glow.set(true);
      form.animMode.set(12);
      form.animStagger.set(0.7F);
      return form;
   }

   private static TypoForm subtitleCard() {
      TypoForm form = new TypoForm();
      form.text.set(BBSFxKeys.PRESET_SUBTITLE.get());
      form.background.set(new Color(0.0F, 0.0F, 0.0F, 0.65F));
      form.padding.set(6.0F);
      form.color.set(Color.white());
      form.wrap.set(480);
      form.align.set(1);
      return form;
   }

   public List<FormCategory> getCategories() {
      return this.fx == null ? Collections.emptyList() : Collections.singletonList(this.fx);
   }
}
