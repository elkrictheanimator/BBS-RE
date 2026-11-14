package bbsfx.forms;

import bbsfx.LangKeys;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Color;

public class TypoForm extends Form {
   public static final Icon ICON = Icons.FONT;
   public static final int ANIM_NONE = 0;
   public static final int ANIM_TYPEWRITER = 1;
   public static final int ANIM_FADE = 2;
   public static final int ANIM_SLIDE_UP = 3;
   public static final int ANIM_SLIDE_DOWN = 4;
   public static final int ANIM_SLIDE_LEFT = 5;
   public static final int ANIM_SLIDE_RIGHT = 6;
   public static final int ANIM_SCALE = 7;
   public static final int ANIM_ROTATE = 8;
   public static final int ANIM_POP = 9;
   public static final int ANIM_WAVE = 10;
   public static final int ANIM_BOUNCE = 11;
   public static final int ANIM_IMPACT = 12;
   public static final IKey[] ALIGN_LABELS = new IKey[]{
      LangKeys.key("bbsfx.ui.align.left"), LangKeys.key("bbsfx.ui.align.center"), LangKeys.key("bbsfx.ui.align.right")
   };
   public static final IKey[] ANIM_LABELS = new IKey[]{
      LangKeys.key("bbsfx.ui.anim.none"),
      LangKeys.key("bbsfx.ui.anim.typewriter"),
      LangKeys.key("bbsfx.ui.anim.fade"),
      LangKeys.key("bbsfx.ui.anim.slide_up"),
      LangKeys.key("bbsfx.ui.anim.slide_down"),
      LangKeys.key("bbsfx.ui.anim.slide_left"),
      LangKeys.key("bbsfx.ui.anim.slide_right"),
      LangKeys.key("bbsfx.ui.anim.scale"),
      LangKeys.key("bbsfx.ui.anim.rotate"),
      LangKeys.key("bbsfx.ui.anim.pop"),
      LangKeys.key("bbsfx.ui.anim.wave"),
      LangKeys.key("bbsfx.ui.anim.bounce"),
      LangKeys.key("bbsfx.ui.anim.impact")
   };
   public static final IKey[] UNIT_LABELS = new IKey[]{LangKeys.key("bbsfx.ui.anim_unit.char"), LangKeys.key("bbsfx.ui.anim_unit.word")};
   public static final IKey[] ORDER_LABELS = new IKey[]{
      LangKeys.key("bbsfx.ui.anim_order.forward"),
      LangKeys.key("bbsfx.ui.anim_order.reverse"),
      LangKeys.key("bbsfx.ui.anim_order.center"),
      LangKeys.key("bbsfx.ui.anim_order.random")
   };
   public static final IKey[] TIME_LABELS = new IKey[]{
      LangKeys.key("bbsfx.ui.time_mode.clamp"), LangKeys.key("bbsfx.ui.time_mode.loop"), LangKeys.key("bbsfx.ui.time_mode.ping_pong")
   };
   public static final IKey[] GRADIENT_LABELS = new IKey[]{LangKeys.key("bbsfx.ui.gradient.horizontal"), LangKeys.key("bbsfx.ui.gradient.vertical")};
   public final ValueString text = new ValueString("text", "BBS FX");
   public final ValueBoolean uppercase = new ValueBoolean("uppercase", false);
   public final ValueLink font = new ValueLink("font", null);
   public final ValueInt fontSize = new ValueInt("fontSize", 9);
   public final ValueFloat tracking = new ValueFloat("tracking", 0.0F, -10.0F, 100.0F);
   public final ValueFloat lineHeight = new ValueFloat("lineHeight", 0.0F, 0.0F, 100.0F);
   public final ValueInt align = new ValueInt("align", 1, 0, 2).modes(ALIGN_LABELS);
   public final ValueInt wrap = new ValueInt("wrap", -1, -1, 10000);
   public final ValueColor color = new ValueColor("color", Color.white());
   public final ValueBoolean gradient = new ValueBoolean("gradient", false);
   public final ValueColor gradientFrom = new ValueColor("gradientFrom", Color.white());
   public final ValueColor gradientTo = new ValueColor("gradientTo", new Color(0.66F, 0.45F, 1.0F, 1.0F));
   public final ValueInt gradientMode = new ValueInt("gradientMode", 0, 0, 1).modes(GRADIENT_LABELS);
   public final ValueColor outlineColor = new ValueColor("outlineColor", new Color(0.0F, 0.0F, 0.0F, 0.0F));
   public final ValueFloat outlineWidth = new ValueFloat("outlineWidth", 1.0F, 0.0F, 8.0F);
   public final ValueBoolean outlineFollow = new ValueBoolean("outlineFollow", false);
   public final ValueColor shadowColor = new ValueColor("shadowColor", new Color(0.0F, 0.0F, 0.0F, 0.0F));
   public final ValueFloat shadowX = new ValueFloat("shadowX", 1.0F, -100.0F, 100.0F);
   public final ValueFloat shadowY = new ValueFloat("shadowY", 1.0F, -100.0F, 100.0F);
   public final ValueBoolean shadowFollow = new ValueBoolean("shadowFollow", false);
   public final ValueColor background = new ValueColor("background", new Color(0.0F, 0.0F, 0.0F, 0.0F));
   public final ValueFloat padding = new ValueFloat("padding", 3.0F, 0.0F, 100.0F);
   public final ValueBoolean glow = new ValueBoolean("glow", false);
   public final ValueColor glowColor = new ValueColor("glowColor", new Color(0.35F, 0.8F, 1.0F, 1.0F));
   public final ValueFloat glowRadius = new ValueFloat("glowRadius", 1.5F, 0.0F, 4.0F);
   public final ValueFloat glowStrength = new ValueFloat("glowStrength", 1.0F, 0.0F, 4.0F);
   public final ValueFloat depth = new ValueFloat("depth", 0.0F, 0.0F, 32.0F);
   public final ValueInt depthSteps = new ValueInt("depthSteps", 8, 1, 32);
   public final ValueColor depthColor = new ValueColor("depthColor", new Color(0.22F, 0.22F, 0.22F, 1.0F));
   public final ValueBoolean billboard = new ValueBoolean("billboard", true);
   public final ValueBoolean billboardYaw = new ValueBoolean("billboardYaw", false);
   public final ValueBoolean model3D = new ValueBoolean("model3D", false);
   public final ValueInt animMode = new ValueInt("animMode", 0, 0, 12).modes(ANIM_LABELS);
   public final ValueInt animUnit = new ValueInt("animUnit", 0, 0, 1).modes(UNIT_LABELS);
   public final ValueFloat animProgress = new ValueFloat("animProgress", 1.0F, 0.0F, 1.0F);
   public final ValueFloat animStagger = new ValueFloat("animStagger", 0.6F, 0.0F, 0.95F);
   public final ValueFloat animAmount = new ValueFloat("animAmount", 1.0F, 0.0F, 10.0F);
   public final ValueBoolean animLoop = new ValueBoolean("animLoop", false);
   public final ValueFloat animSpeed = new ValueFloat("animSpeed", 1.0F, 0.01F, 20.0F);
   public final ValueInt animOrder = new ValueInt("animOrder", 0, 0, 3).modes(ORDER_LABELS);
   public final ValueInt animSeed = new ValueInt("animSeed", 1, -1000000, 1000000);
   public final ValueBoolean timeRemap = new ValueBoolean("timeRemap", false);
   public final ValueFloat timeSpeed = new ValueFloat("timeSpeed", 1.0F, -20.0F, 20.0F);
   public final ValueFloat timeOffset = new ValueFloat("timeOffset", 0.0F, -100000.0F, 100000.0F);
   public final ValueFloat timeDuration = new ValueFloat("timeDuration", 40.0F, 0.01F, 100000.0F);
   public final ValueInt timeMode = new ValueInt("timeMode", 0, 0, 2).modes(TIME_LABELS);
   public final ValueFloat easeX1 = new ValueFloat("easeX1", 0.25F, 0.0F, 1.0F);
   public final ValueFloat easeY1 = new ValueFloat("easeY1", 0.1F, -1.0F, 2.0F);
   public final ValueFloat easeX2 = new ValueFloat("easeX2", 0.25F, 0.0F, 1.0F);
   public final ValueFloat easeY2 = new ValueFloat("easeY2", 1.0F, -1.0F, 2.0F);

   public TypoForm() {
      this.add(this.text);
      this.add(this.uppercase);
      this.add(this.font);
      this.add(this.fontSize);
      this.add(this.tracking);
      this.add(this.lineHeight);
      this.add(this.align);
      this.add(this.wrap);
      this.add(this.color);
      this.add(this.gradient);
      this.add(this.gradientFrom);
      this.add(this.gradientTo);
      this.add(this.gradientMode);
      this.add(this.outlineColor);
      this.add(this.outlineWidth);
      this.add(this.outlineFollow);
      this.add(this.shadowColor);
      this.add(this.shadowX);
      this.add(this.shadowY);
      this.add(this.shadowFollow);
      this.add(this.background);
      this.add(this.padding);
      this.add(this.glow);
      this.add(this.glowColor);
      this.add(this.glowRadius);
      this.add(this.glowStrength);
      this.add(this.depth);
      this.add(this.depthSteps);
      this.add(this.depthColor);
      this.add(this.billboard);
      this.add(this.billboardYaw);
      this.add(this.model3D);
      this.add(this.animMode);
      this.add(this.animUnit);
      this.add(this.animProgress);
      this.add(this.animStagger);
      this.add(this.animAmount);
      this.add(this.animLoop);
      this.add(this.animSpeed);
      this.add(this.animOrder);
      this.add(this.animSeed);
      this.add(this.timeRemap);
      this.add(this.timeSpeed);
      this.add(this.timeOffset);
      this.add(this.timeDuration);
      this.add(this.timeMode);
      this.add(this.easeX1);
      this.add(this.easeY1);
      this.add(this.easeX2);
      this.add(this.easeY2);
      this.outlineFollow.invisible();
      this.shadowFollow.invisible();
      this.billboardYaw.invisible();
      this.model3D.invisible();
      this.animUnit.invisible();
      this.glow.invisible();
      this.glowColor.invisible();
      this.glowRadius.invisible();
      this.glowStrength.invisible();
      this.depth.invisible();
      this.depthSteps.invisible();
      this.depthColor.invisible();
   }

   public String getDefaultDisplayName() {
      String text = (String)this.text.get();
      return text.isEmpty() ? LangKeys.key("bbsfx.form.typo").get() : text;
   }

   public Icon getIcon() {
      return ICON;
   }
}
