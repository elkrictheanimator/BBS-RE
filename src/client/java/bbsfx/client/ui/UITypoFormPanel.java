package bbsfx.client.ui;

import bbsfx.forms.TypoForm;
import java.io.File;
import mchorse.bbs_mod.fonts.FontManager;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.forms.editors.forms.UIForm;
import mchorse.bbs_mod.ui.forms.editors.panels.UIFormPanel;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.values.UIValues;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UITypoFormPanel extends UIFormPanel<TypoForm> {
   public UIColor color;
   public UIColor gradientFrom;
   public UIColor gradientTo;
   public UIColor outlineColor;
   public UIColor shadowColor;
   public UIColor background;
   public UIColor glowColor;
   public UIColor depthColor;

   public UITypoFormPanel(UIForm editor) {
      super(editor);
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.TYPO_TEXT),
               UIValues.textbox(10000, () -> ((TypoForm)this.form).text),
               BBSFxKeys.hint(BBSFxKeys.TYPO_MARKUP_HINT),
               UIValues.toggle(BBSFxKeys.TYPO_UPPERCASE, () -> ((TypoForm)this.form).uppercase)
            }
         );
      UIButton pickFont = new UIButton(
         UIKeys.FORMS_EDITORS_LABEL_FONT_PICK,
         b -> {
            UIStringOverlayPanel panel = UIStringOverlayPanel.links(
               UIKeys.FORMS_EDITORS_LABEL_FONT_PICK, FontManager.getFontLinks(), l -> ((TypoForm)this.form).font.set(l)
            );
            UIOverlay.addOverlay(this.getContext(), panel.set((Link)((TypoForm)this.form).font.get()));
         }
      );
      UIIcon openFontFolder = new UIIcon(Icons.FOLDER, b -> {
         File folder = FontManager.getFolder();
         folder.mkdirs();
         UIUtils.openFolder(folder);
      });
      UITrackpad fontSize = UIValues.trackpad(() -> ((TypoForm)this.form).fontSize);
      UITrackpad lineHeight = UIValues.trackpad(() -> ((TypoForm)this.form).lineHeight);
      UITrackpad tracking = UIValues.trackpad(() -> ((TypoForm)this.form).tracking);
      UITrackpad wrap = UIValues.trackpad(() -> ((TypoForm)this.form).wrap);
      fontSize.limit(4.0, 96.0, true).tooltip(BBSFxKeys.TYPO_FONT_SIZE);
      lineHeight.values(0.05F).tooltip(BBSFxKeys.TYPO_LINE_HEIGHT);
      tracking.values(0.1F, 0.01F, 0.5).tooltip(BBSFxKeys.TYPO_TRACKING);
      wrap.increment(10.0).tooltip(BBSFxKeys.TYPO_WRAP);
      UICirculate align = this.modeCycler();
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.TYPO_FONT).marginTop(3),
               UI.row(new UIElement[]{pickFont, openFontFolder}),
               UI.row(new UIElement[]{fontSize, lineHeight}),
               UI.row(new UIElement[]{tracking, wrap}),
               UI.labelRow(BBSFxKeys.TYPO_ALIGN, align)
            }
         );
      this.color = UIValues.color(() -> ((TypoForm)this.form).color).withAlpha();
      this.gradientFrom = UIValues.color(() -> ((TypoForm)this.form).gradientFrom).withAlpha();
      this.gradientTo = UIValues.color(() -> ((TypoForm)this.form).gradientTo).withAlpha();
      this.outlineColor = UIValues.color(() -> ((TypoForm)this.form).outlineColor).withAlpha();
      this.shadowColor = UIValues.color(() -> ((TypoForm)this.form).shadowColor).withAlpha();
      this.background = UIValues.color(() -> ((TypoForm)this.form).background).withAlpha();
      UICirculate gradientMode = this.gradientModeCycler();
      UITrackpad outlineWidth = UIValues.trackpad(() -> ((TypoForm)this.form).outlineWidth);
      UITrackpad shadowX = UIValues.trackpad(() -> ((TypoForm)this.form).shadowX);
      UITrackpad shadowY = UIValues.trackpad(() -> ((TypoForm)this.form).shadowY);
      UITrackpad padding = UIValues.trackpad(() -> ((TypoForm)this.form).padding);
      outlineWidth.values(0.1F, 0.05F, 0.5).tooltip(BBSFxKeys.TYPO_OUTLINE_WIDTH);
      shadowX.values(0.1F, 0.01F, 0.5).tooltip(BBSFxKeys.TYPO_SHADOW_OFFSET);
      shadowY.values(0.1F, 0.01F, 0.5).tooltip(BBSFxKeys.TYPO_SHADOW_OFFSET);
      padding.tooltip(BBSFxKeys.TYPO_PADDING);
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.TYPO_APPEARANCE).marginTop(3),
               UI.labelRow(BBSFxKeys.TYPO_COLOR, this.color),
               UIValues.toggle(BBSFxKeys.GRADIENT, () -> ((TypoForm)this.form).gradient),
               UI.row(new UIElement[]{this.gradientFrom, this.gradientTo}),
               UI.labelRow(BBSFxKeys.GRADIENT_MODE, gradientMode),
               UI.labelRow(BBSFxKeys.TYPO_OUTLINE_COLOR, this.outlineColor),
               outlineWidth,
               UIValues.toggle(BBSFxKeys.TYPO_OUTLINE_FOLLOW, () -> ((TypoForm)this.form).outlineFollow),
               UI.labelRow(BBSFxKeys.TYPO_SHADOW_COLOR, this.shadowColor),
               UI.row(new UIElement[]{shadowX, shadowY}),
               UIValues.toggle(BBSFxKeys.TYPO_SHADOW_FOLLOW, () -> ((TypoForm)this.form).shadowFollow),
               UI.labelRow(BBSFxKeys.TYPO_BACKGROUND, this.background),
               padding
            }
         );
      this.glowColor = UIValues.color(() -> ((TypoForm)this.form).glowColor).withAlpha();
      this.depthColor = UIValues.color(() -> ((TypoForm)this.form).depthColor).withAlpha();
      UITrackpad glowRadius = UIValues.trackpad(() -> ((TypoForm)this.form).glowRadius);
      UITrackpad glowStrength = UIValues.trackpad(() -> ((TypoForm)this.form).glowStrength);
      UITrackpad depth = UIValues.trackpad(() -> ((TypoForm)this.form).depth);
      UITrackpad depthSteps = UIValues.trackpad(() -> ((TypoForm)this.form).depthSteps);
      glowRadius.values(0.1F, 0.05F, 0.5).tooltip(BBSFxKeys.TYPO_GLOW_RADIUS);
      glowStrength.values(0.05F).tooltip(BBSFxKeys.TYPO_GLOW_STRENGTH);
      depth.values(0.1F, 0.05F, 0.5).tooltip(BBSFxKeys.TYPO_DEPTH);
      depthSteps.values(1.0).tooltip(BBSFxKeys.TYPO_DEPTH_STEPS);
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.TYPO_POST).marginTop(3),
               UIValues.toggle(BBSFxKeys.TYPO_GLOW, () -> ((TypoForm)this.form).glow),
               UI.labelRow(BBSFxKeys.TYPO_GLOW_COLOR, this.glowColor),
               UI.row(new UIElement[]{glowRadius, glowStrength}),
               UI.label(BBSFxKeys.TYPO_DEPTH).marginTop(3),
               UI.row(new UIElement[]{depth, depthSteps}),
               UI.labelRow(BBSFxKeys.TYPO_DEPTH_COLOR, this.depthColor),
               BBSFxKeys.hint(BBSFxKeys.TYPO_DEPTH_HINT)
            }
         );
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.TYPO_TRACKING3D).marginTop(3),
               UI.labelRow(BBSFxKeys.TYPO_BILLBOARD, this.billboardCycler()),
               UIValues.toggle(BBSFxKeys.TYPO_MODEL3D, () -> ((TypoForm)this.form).model3D),
               BBSFxKeys.hint(BBSFxKeys.TYPO_MODEL3D_HINT),
               BBSFxKeys.hint(BBSFxKeys.TYPO_ANCHOR_HINT)
            }
         );
      UICirculate animMode = this.animModeCycler();
      UICirculate animUnit = this.animUnitCycler();
      UITrackpad animProgress = UIValues.trackpad(() -> ((TypoForm)this.form).animProgress);
      UITrackpad animStagger = UIValues.trackpad(() -> ((TypoForm)this.form).animStagger);
      UITrackpad animAmount = UIValues.trackpad(() -> ((TypoForm)this.form).animAmount);
      UITrackpad animSpeed = UIValues.trackpad(() -> ((TypoForm)this.form).animSpeed);
      animProgress.values(0.01F).tooltip(BBSFxKeys.TYPO_ANIM_PROGRESS);
      animStagger.values(0.01F).tooltip(BBSFxKeys.TYPO_ANIM_STAGGER);
      animAmount.values(0.1F, 0.05F, 0.5).tooltip(BBSFxKeys.TYPO_ANIM_AMOUNT);
      animSpeed.values(0.05F).tooltip(BBSFxKeys.TYPO_ANIM_SPEED);
      this.options
         .add(
            new IUIElement[]{
               UI.label(BBSFxKeys.TYPO_ANIM).marginTop(3),
               UI.labelRow(BBSFxKeys.TYPO_ANIM_MODE, animMode),
               UI.labelRow(BBSFxKeys.TYPO_ANIM_UNIT, animUnit),
               UI.row(new UIElement[]{animProgress, animStagger}),
               UI.row(new UIElement[]{animAmount, animSpeed}),
               UIValues.toggle(BBSFxKeys.TYPO_ANIM_LOOP, () -> ((TypoForm)this.form).animLoop)
            }
         );
      this.options.add(new IUIElement[]{UI.label(BBSFxKeys.TYPO_EASE).marginTop(3), this.easeCurve()});
   }

   private UIBezierCurve easeCurve() {
      UIBezierCurve curve = new UIBezierCurve(
         () -> this.form == null
            ? UIBezierCurve.EASE
            : new float[]{
               (Float)((TypoForm)this.form).easeX1.get(),
               (Float)((TypoForm)this.form).easeY1.get(),
               (Float)((TypoForm)this.form).easeX2.get(),
               (Float)((TypoForm)this.form).easeY2.get()
            },
         v -> {
            if (this.form != null) {
               ((TypoForm)this.form).easeX1.set(v[0]);
               ((TypoForm)this.form).easeY1.set(v[1]);
               ((TypoForm)this.form).easeX2.set(v[2]);
               ((TypoForm)this.form).easeY2.set(v[3]);
            }
         }
      );
      curve.h(110);
      return curve;
   }

   private UICirculate modeCycler() {
      UICirculate circulate = new UICirculate(null);

      for (IKey key : TypoForm.ALIGN_LABELS) {
         circulate.addLabel(key);
      }

      circulate.callback = b -> ((TypoForm)this.form).align.set(circulate.getValue());
      circulate.valueBinding(() -> circulate.setValue((Integer)((TypoForm)this.form).align.get()));
      circulate.w(90);
      return circulate;
   }

   private UICirculate gradientModeCycler() {
      UICirculate circulate = new UICirculate(null);

      for (IKey key : TypoForm.GRADIENT_LABELS) {
         circulate.addLabel(key);
      }

      circulate.callback = b -> ((TypoForm)this.form).gradientMode.set(circulate.getValue());
      circulate.valueBinding(() -> circulate.setValue((Integer)((TypoForm)this.form).gradientMode.get()));
      circulate.w(90);
      return circulate;
   }

   private UICirculate billboardCycler() {
      UICirculate circulate = new UICirculate(null);
      circulate.addLabel(BBSFxKeys.TYPO_BILLBOARD_OFF);
      circulate.addLabel(BBSFxKeys.TYPO_BILLBOARD_FULL);
      circulate.addLabel(BBSFxKeys.TYPO_BILLBOARD_YAW);
      circulate.callback = b -> {
         int mode = circulate.getValue();
         ((TypoForm)this.form).billboard.set(mode != 0);
         ((TypoForm)this.form).billboardYaw.set(mode == 2);
      };
      circulate.valueBinding(() -> {
         int mode = !((TypoForm)this.form).billboard.get() ? 0 : (((TypoForm)this.form).billboardYaw.get() ? 2 : 1);
         circulate.setValue(mode);
      });
      circulate.w(100);
      return circulate;
   }

   private UICirculate animModeCycler() {
      UICirculate circulate = new UICirculate(null);

      for (IKey key : TypoForm.ANIM_LABELS) {
         circulate.addLabel(key);
      }

      circulate.callback = b -> ((TypoForm)this.form).animMode.set(circulate.getValue());
      circulate.valueBinding(() -> circulate.setValue((Integer)((TypoForm)this.form).animMode.get()));
      circulate.w(90);
      return circulate;
   }

   private UICirculate animUnitCycler() {
      UICirculate circulate = new UICirculate(null);

      for (IKey key : TypoForm.UNIT_LABELS) {
         circulate.addLabel(key);
      }

      circulate.callback = b -> ((TypoForm)this.form).animUnit.set(circulate.getValue());
      circulate.valueBinding(() -> circulate.setValue((Integer)((TypoForm)this.form).animUnit.get()));
      circulate.w(80);
      return circulate;
   }

   public void finishEdit() {
      super.finishEdit();
      this.color.picker.removeFromParent();
      this.outlineColor.picker.removeFromParent();
      this.shadowColor.picker.removeFromParent();
      this.background.picker.removeFromParent();
      this.glowColor.picker.removeFromParent();
      this.depthColor.picker.removeFromParent();
   }
}
