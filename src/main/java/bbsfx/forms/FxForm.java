package bbsfx.forms;

import bbsfx.LangKeys;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;

public class FxForm extends Form {
   public static final Icon ICON = Icons.SHAPES;
   public static final int FX_SHOCKWAVE = 0;
   public static final int FX_RING = 1;
   public static final int FX_SPARKS = 2;
   public static final int FX_RAYS = 3;
   public static final int FX_ORB = 4;
   public static final int FX_FIELD = 5;
   public static final int FX_TRAIL = 6;
   public static final int FX_LIGHTNING = 7;
   public static final int FX_SPIRAL = 8;
   public static final int FX_STARS = 9;
   public static final int FX_VORTEX = 10;
   public static final int FX_GRID = 11;
   public static final IKey[] EFFECT_LABELS = new IKey[]{
      LangKeys.key("bbsfx.ui.fx.effect.shockwave"),
      LangKeys.key("bbsfx.ui.fx.effect.ring"),
      LangKeys.key("bbsfx.ui.fx.effect.sparks"),
      LangKeys.key("bbsfx.ui.fx.effect.rays"),
      LangKeys.key("bbsfx.ui.fx.effect.orb"),
      LangKeys.key("bbsfx.ui.fx.effect.field"),
      LangKeys.key("bbsfx.ui.fx.effect.trail"),
      LangKeys.key("bbsfx.ui.fx.effect.lightning"),
      LangKeys.key("bbsfx.ui.fx.effect.spiral"),
      LangKeys.key("bbsfx.ui.fx.effect.stars"),
      LangKeys.key("bbsfx.ui.fx.effect.vortex"),
      LangKeys.key("bbsfx.ui.fx.effect.grid")
   };
   public final ValueInt effect = new ValueInt("effect", 0, 0, 11).modes(EFFECT_LABELS);
   public final ValueColor primary = new ValueColor("primary", Color.white());
   public final ValueColor secondary = new ValueColor("secondary", new Color(0.0F, 0.63F, 1.0F, 1.0F));
   public final ValueFloat size = new ValueFloat("size", 2.0F, 0.01F, 64.0F);
   public final ValueFloat progress = new ValueFloat("progress", 0.25F, 0.0F, 1.0F);
   public final ValueBoolean loop = new ValueBoolean("loop", false);
   public final ValueFloat speed = new ValueFloat("speed", 1.0F, 0.01F, 20.0F);
   public final ValueFloat intensity = new ValueFloat("intensity", 1.0F, 0.0F, 8.0F);
   public final ValueBoolean billboard = new ValueBoolean("billboard", true);
   public final ValueBoolean additive = new ValueBoolean("additive", true);
   public final ValueInt count = new ValueInt("count", 24, 3, 64);
   public final ValueFloat thickness = new ValueFloat("thickness", 0.12F, 0.01F, 0.6F);
   public final ValueFloat spin = new ValueFloat("spin", 0.0F, -4.0F, 4.0F);
   public final ValueInt layers = new ValueInt("layers", 1, 1, 6);
   public final ValueFloat height = new ValueFloat("height", 0.0F, 0.0F, 4.0F);
   public final ValueFloat flicker = new ValueFloat("flicker", 1.0F, 0.0F, 8.0F);
   public final ValueFloat colorMix = new ValueFloat("colorMix", 0.5F, 0.0F, 1.0F);
   public final ValueFloat fade = new ValueFloat("fade", 1.0F, 0.2F, 3.0F);

   public FxForm() {
      this.add(this.effect);
      this.add(this.primary);
      this.add(this.secondary);
      this.add(this.size);
      this.add(this.progress);
      this.add(this.loop);
      this.add(this.speed);
      this.add(this.intensity);
      this.add(this.billboard);
      this.add(this.additive);
      this.add(this.count);
      this.add(this.thickness);
      this.add(this.spin);
      this.add(this.layers);
      this.add(this.height);
      this.add(this.flicker);
      this.add(this.colorMix);
      this.add(this.fade);
   }

   public String getDefaultDisplayName() {
      return LangKeys.key("bbsfx.form.fx").get();
   }

   public Icon getIcon() {
      return ICON;
   }
}
