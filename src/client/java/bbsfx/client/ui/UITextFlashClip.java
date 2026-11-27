package bbsfx.client.ui;

import bbsfx.text.TextFlashClip;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.fonts.FontManager;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UITextFlashClip extends UIMusicTextClip<TextFlashClip> {
   private UITrackpad size;
   private UITrackpad x;
   private UITrackpad y;
   private UITrackpad rowLength;
   private UITrackpad tracking;
   private UITrackpad lineHeight;
   private UIToggle uppercase;
   private UICirculate layout;
   private UIButton pickFont;
   private UIButton pickActor;
   private UIColor white;
   private UIToggle gradient;
   private UIColor gradientFrom;
   private UIColor gradientTo;
   private UICirculate gradientMode;
   private UIColor red;
   private UIColor outline;
   private UITrackpad outlineWidth;
   private UITrackpad redChance;
   private UITrackpad glowChance;
   private UITrackpad glowStrength;
   private UITrackpad slamChance;
   private UITrackpad tilt;
   private UITrackpad beat;
   private UITrackpad beatSpin;
   private UITrackpad beatFlash;
   private UITrackpad wave;
   private UITrackpad waveSpeed;
   private UITrackpad strobe;

   public UITextFlashClip(TextFlashClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.size = this.number(((TextFlashClip)this.clip).size, BBSFxKeys.TF_SIZE).forcedLabel(BBSFxKeys.TF_SIZE_SHORT);
      this.x = this.number(((TextFlashClip)this.clip).x, BBSFxKeys.TF_X).forcedLabel(IKey.raw("X"));
      this.y = this.number(((TextFlashClip)this.clip).y, BBSFxKeys.TF_Y).forcedLabel(IKey.raw("Y"));
      this.rowLength = this.number(((TextFlashClip)this.clip).rowLength, BBSFxKeys.TF_ROW_LENGTH).forcedLabel(BBSFxKeys.TF_ROW_LENGTH_SHORT);
      this.tracking = this.number(((TextFlashClip)this.clip).tracking, BBSFxKeys.TF_TRACKING).forcedLabel(BBSFxKeys.TF_TRACKING_SHORT);
      this.lineHeight = this.number(((TextFlashClip)this.clip).lineHeight, BBSFxKeys.TF_LINE_HEIGHT).forcedLabel(BBSFxKeys.TF_LINE_HEIGHT_SHORT);
      this.uppercase = this.toggle(BBSFxKeys.TF_UPPERCASE, ((TextFlashClip)this.clip).uppercase);
      this.layout = new UICirculate(null);

      for (IKey label : new IKey[]{BBSFxKeys.TF_LAYOUT_JUSTIFIED, BBSFxKeys.TF_LAYOUT_NATURAL, BBSFxKeys.TF_LAYOUT_LEFT}) {
         this.layout.addLabel(label);
      }

      this.layout.callback = c -> this.editor.editMultiple(((TextFlashClip)this.clip).layout, v -> v.set(c.getValue()));
      this.layout.setValue((Integer)((TextFlashClip)this.clip).layout.get());
      this.layout.w(90);
      this.bind(this.layout, () -> this.layout.setValue((Integer)((TextFlashClip)this.clip).layout.get()));
      this.pickFont = new UIButton(
         UIKeys.CAMERA_PANELS_SUBTITLE_FONT_PICK,
         b -> {
            UIStringOverlayPanel panel = UIStringOverlayPanel.links(
               UIKeys.CAMERA_PANELS_SUBTITLE_FONT_PICK,
               FontManager.getFontLinks(),
               l -> this.editor.editMultiple(((TextFlashClip)this.clip).font, v -> v.set(l))
            );
            UIOverlay.addOverlay(this.getContext(), panel.set((Link)((TextFlashClip)this.clip).font.get()));
         }
      );
      this.pickFont.tooltip(BBSFxKeys.TF_FONT_HINT);
      this.pickActor = new UIButton(BBSFxKeys.TF_ACTOR_NONE, b -> this.cycleActor());
      this.pickActor.tooltip(BBSFxKeys.TF_ACTOR_HINT);
      this.white = this.color(((TextFlashClip)this.clip).white).withAlpha();
      this.gradient = this.toggle(BBSFxKeys.GRADIENT, ((TextFlashClip)this.clip).gradient);
      this.gradientFrom = this.color(((TextFlashClip)this.clip).gradientFrom).withAlpha();
      this.gradientTo = this.color(((TextFlashClip)this.clip).gradientTo).withAlpha();
      this.gradientMode = this.modeCycler(new IKey[]{BBSFxKeys.GRADIENT_HORIZONTAL, BBSFxKeys.GRADIENT_VERTICAL}, ((TextFlashClip)this.clip).gradientMode);
      this.red = this.color(((TextFlashClip)this.clip).red).withAlpha();
      this.outline = this.color(((TextFlashClip)this.clip).outline).withAlpha();
      this.outlineWidth = this.number(((TextFlashClip)this.clip).outlineWidth, BBSFxKeys.TF_OUTLINE_WIDTH).forcedLabel(BBSFxKeys.TF_OUTLINE_WIDTH_SHORT);
      this.redChance = this.number(((TextFlashClip)this.clip).redChance, BBSFxKeys.TF_RED_CHANCE).forcedLabel(BBSFxKeys.TF_RED_CHANCE_SHORT);
      this.glowChance = this.number(((TextFlashClip)this.clip).glowChance, BBSFxKeys.TF_GLOW_CHANCE).forcedLabel(BBSFxKeys.TF_GLOW_CHANCE_SHORT);
      this.glowStrength = this.number(((TextFlashClip)this.clip).glowStrength, BBSFxKeys.TF_GLOW_STRENGTH).forcedLabel(BBSFxKeys.TF_GLOW_STRENGTH_SHORT);
      this.slamChance = this.number(((TextFlashClip)this.clip).slamChance, BBSFxKeys.TF_SLAM_CHANCE).forcedLabel(BBSFxKeys.TF_SLAM_CHANCE_SHORT);
      this.tilt = this.number(((TextFlashClip)this.clip).tilt, BBSFxKeys.TF_TILT).forcedLabel(BBSFxKeys.TF_TILT_SHORT);
      this.beat = this.number(((TextFlashClip)this.clip).beat, BBSFxKeys.TF_BEAT).forcedLabel(BBSFxKeys.TF_BEAT_SHORT);
      this.beatSpin = this.number(((TextFlashClip)this.clip).beatSpin, BBSFxKeys.TF_BEAT_SPIN).forcedLabel(BBSFxKeys.TF_BEAT_SPIN_SHORT);
      this.beatFlash = this.number(((TextFlashClip)this.clip).beatFlash, BBSFxKeys.TF_BEAT_FLASH).forcedLabel(BBSFxKeys.TF_BEAT_FLASH_SHORT);
      this.wave = this.number(((TextFlashClip)this.clip).wave, BBSFxKeys.TF_WAVE).forcedLabel(BBSFxKeys.TF_WAVE_SHORT);
      this.waveSpeed = this.number(((TextFlashClip)this.clip).waveSpeed, BBSFxKeys.TF_WAVE_SPEED).forcedLabel(BBSFxKeys.TF_WAVE_SPEED_SHORT);
      this.strobe = this.number(((TextFlashClip)this.clip).strobe, BBSFxKeys.TF_STROBE).forcedLabel(BBSFxKeys.TF_STROBE_SHORT);
      this.refreshActorLabel();
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.TF_LAYOUT,
               BBSFxKeys.TF_LAYOUT_HINT,
               UI.row(new UIElement[]{this.size, this.rowLength}),
               UI.row(new UIElement[]{this.tracking, this.lineHeight}),
               UI.row(new UIElement[]{this.x, this.y}),
               UI.labelRow(BBSFxKeys.TF_LAYOUT_MODE, this.layout),
               UI.row(new UIElement[]{this.uppercase, this.pickFont}),
               this.pickActor
            )
         );
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.TF_COLORS,
               BBSFxKeys.TF_COLORS_HINT,
               UI.labelRow(BBSFxKeys.TF_WHITE, this.white),
               UI.row(new UIElement[]{this.gradient, this.gradientMode}),
               UI.row(new UIElement[]{this.gradientFrom, this.gradientTo}),
               UI.labelRow(BBSFxKeys.TF_RED, this.red),
               UI.labelRow(BBSFxKeys.TF_OUTLINE, this.outline),
               this.outlineWidth,
               UI.row(new UIElement[]{this.redChance, this.glowChance}),
               this.glowStrength
            )
         );
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.TF_MOTION,
               BBSFxKeys.TF_HINT,
               UI.row(new UIElement[]{this.slamChance, this.tilt}),
               this.beat,
               UI.row(new UIElement[]{this.beatSpin, this.beatFlash}),
               UI.row(new UIElement[]{this.wave, this.waveSpeed}),
               this.strobe
            )
         );
   }

   private void cycleActor() {
      List<String> ids = new ArrayList();
      ids.add("");
      Film film = this.editor.getFilm();
      if (film != null) {
         for (Replay replay : film.replays.getList()) {
            String id = replay.getId();
            if (id != null && !id.isEmpty()) {
               ids.add(id);
            }
         }
      }

      String current = (String)((TextFlashClip)this.clip).actor.get();
      int index = ids.indexOf(current);
      String next = (String)ids.get((index + 1) % ids.size());
      this.editor.editMultiple(((TextFlashClip)this.clip).actor, v -> v.set(next));
      this.refreshActorLabel();
   }

   private void refreshActorLabel() {
      String id = (String)((TextFlashClip)this.clip).actor.get();
      IKey label;
      if (id != null && !id.isEmpty()) {
         Film film = this.editor.getFilm();
         String name = id;
         if (film != null) {
            for (Replay replay : film.replays.getList()) {
               if (id.equals(replay.getId())) {
                  String text = (String)replay.label.get();
                  if (text != null && !text.isEmpty()) {
                     name = text;
                  }
                  break;
               }
            }
         }

         label = IKey.raw("Actor: " + name);
      } else {
         label = BBSFxKeys.TF_ACTOR_NONE;
      }

      this.pickActor.label = label;
   }
}
