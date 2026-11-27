package bbsfx.client.ui;

import bbsfx.lyrics.Lrc;
import bbsfx.lyrics.LyricSplit;
import bbsfx.text.Lyrics3DClip;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.fonts.FontManager;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.utils.UILabel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UILyrics3DClip extends UIMusicTextClip<Lyrics3DClip> {
   private static final int[] GLOW_PRESETS = new int[]{-1, -41830, -11098369, -17848};
   private UITrackpad distance;
   private UITrackpad spread;
   private UITrackpad drop;
   private UITrackpad positionX;
   private UITrackpad positionY;
   private UITrackpad positionZ;
   private UITrackpad yawOffset;
   private UITrackpad pitchOffset;
   private UITrackpad roll;
   private UITrackpad scale;
   private UIToggle faceCamera;
   private UIToggle keepAway;
   private UIToggle aboveGround;
   private UIToggle throughWalls;
   private UIToggle followCamera;
   private UIButton pickActor;
   private UIToggle smoothFont;
   private UICirculate fontStyle;
   private UIToggle uppercase;
   private UIToggle bilingual;
   private UIToggle gradient;
   private UIColor gradientFrom;
   private UIColor gradientTo;
   private UICirculate gradientMode;
   private UIButton pickTranslation;
   private UIToggle swapTranslation;
   private UILabel translationStatus;
   private UIElement translationRow;
   private UICirculate intro;
   private UICirculate charFx;
   private UICirculate align;
   private UIButton pickFont;
   private UIColor color;
   private UIToggle textShadow;
   private UITrackpad maxWidth;
   private UIToggle glow;
   private UIColor glowColor;
   private UITrackpad glowStrength;
   private UITrackpad glowRadius;
   private UIToggle wiggle;
   private UITrackpad wiggleFps;
   private UITrackpad wiggleAmount;
   private UIToggle wordPop;
   private UITrackpad beat;

   public UILyrics3DClip(Lyrics3DClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   @Override
   protected void registerUI() {
      super.registerUI();
      this.distance = this.number(((Lyrics3DClip)this.clip).distance, BBSFxKeys.L3D_DISTANCE).forcedLabel(BBSFxKeys.L3D_DISTANCE_SHORT);
      this.spread = this.number(((Lyrics3DClip)this.clip).spread, BBSFxKeys.L3D_SPREAD).forcedLabel(BBSFxKeys.L3D_SPREAD_SHORT);
      this.drop = this.number(((Lyrics3DClip)this.clip).drop, BBSFxKeys.L3D_DROP).forcedLabel(BBSFxKeys.L3D_DROP_SHORT);
      this.positionX = this.number(((Lyrics3DClip)this.clip).positionX, BBSFxKeys.L3D_POSITION_X).forcedLabel(IKey.raw("X"));
      this.positionY = this.number(((Lyrics3DClip)this.clip).positionY, BBSFxKeys.L3D_POSITION_Y).forcedLabel(IKey.raw("Y"));
      this.positionZ = this.number(((Lyrics3DClip)this.clip).positionZ, BBSFxKeys.L3D_POSITION_Z).forcedLabel(IKey.raw("Z"));
      this.yawOffset = this.number(((Lyrics3DClip)this.clip).yawOffset, BBSFxKeys.L3D_YAW).forcedLabel(BBSFxKeys.L3D_YAW_SHORT);
      this.pitchOffset = this.number(((Lyrics3DClip)this.clip).pitchOffset, BBSFxKeys.L3D_PITCH).forcedLabel(BBSFxKeys.L3D_PITCH_SHORT);
      this.roll = this.number(((Lyrics3DClip)this.clip).roll, BBSFxKeys.L3D_ROLL).forcedLabel(BBSFxKeys.L3D_ROLL_SHORT);
      this.scale = this.number(((Lyrics3DClip)this.clip).scale, BBSFxKeys.L3D_SCALE).forcedLabel(BBSFxKeys.L3D_SCALE_SHORT);
      this.faceCamera = this.toggle(BBSFxKeys.L3D_FACE_CAMERA, ((Lyrics3DClip)this.clip).faceCamera);
      this.keepAway = this.toggle(BBSFxKeys.L3D_KEEP_AWAY, ((Lyrics3DClip)this.clip).keepAway);
      this.aboveGround = this.toggle(BBSFxKeys.L3D_ABOVE_GROUND, ((Lyrics3DClip)this.clip).aboveGround);
      this.throughWalls = this.toggle(BBSFxKeys.L3D_THROUGH_WALLS, ((Lyrics3DClip)this.clip).throughWalls);
      this.followCamera = this.toggle(BBSFxKeys.L3D_FOLLOW_CAMERA, ((Lyrics3DClip)this.clip).followCamera);
      this.followCamera.tooltip(BBSFxKeys.L3D_FOLLOW_HINT);
      this.pickActor = new UIButton(BBSFxKeys.TF_ACTOR_NONE, b -> this.cycleActor());
      this.pickActor.tooltip(BBSFxKeys.TF_ACTOR_HINT);
      this.smoothFont = this.toggle(BBSFxKeys.L3D_SMOOTH_FONT, ((Lyrics3DClip)this.clip).smoothFont);
      this.fontStyle = this.cycler(
         new IKey[]{BBSFxKeys.L3D_FONT_STYLE_PLAIN, BBSFxKeys.L3D_FONT_STYLE_BOLD, BBSFxKeys.L3D_FONT_STYLE_DISPLAY}, ((Lyrics3DClip)this.clip).fontStyle
      );
      this.uppercase = this.toggle(BBSFxKeys.L3D_UPPERCASE, ((Lyrics3DClip)this.clip).uppercase);
      this.bilingual = this.toggle(BBSFxKeys.L3D_BILINGUAL, ((Lyrics3DClip)this.clip).bilingual);
      this.pickTranslation = new UILyrics3DClip.TranslationButton();
      this.swapTranslation = this.toggle(BBSFxKeys.L3D_SWAP_TRANSLATION, ((Lyrics3DClip)this.clip).swapTranslation);
      this.swapTranslation.tooltip(BBSFxKeys.L3D_SWAP_TRANSLATION_HINT, Direction.BOTTOM);
      this.translationStatus = new UILabel(BBSFxKeys.MUSIC_TRANSLATION_NONE);
      this.intro = this.cycler(
         new IKey[]{
            BBSFxKeys.L3D_INTRO_FADE,
            BBSFxKeys.L3D_INTRO_DROP,
            BBSFxKeys.L3D_INTRO_RISE,
            BBSFxKeys.L3D_INTRO_POP,
            BBSFxKeys.L3D_INTRO_FLICKER,
            BBSFxKeys.L3D_INTRO_SLIDE_LEFT,
            BBSFxKeys.L3D_INTRO_SLIDE_RIGHT,
            BBSFxKeys.L3D_INTRO_SLAM
         },
         ((Lyrics3DClip)this.clip).intro
      );
      this.charFx = this.cycler(
         new IKey[]{BBSFxKeys.L3D_CHAR_FX_STANDARD, BBSFxKeys.L3D_CHAR_FX_SLAM, BBSFxKeys.L3D_CHAR_FX_ROLL}, ((Lyrics3DClip)this.clip).charFx
      );
      this.align = this.cycler(new IKey[]{BBSFxKeys.L3D_ALIGN_CENTER, BBSFxKeys.L3D_ALIGN_LEFT, BBSFxKeys.L3D_ALIGN_RIGHT}, ((Lyrics3DClip)this.clip).align);
      this.pickFont = new UIButton(
         UIKeys.CAMERA_PANELS_SUBTITLE_FONT_PICK,
         b -> {
            UIStringOverlayPanel panel = UIStringOverlayPanel.links(
               UIKeys.CAMERA_PANELS_SUBTITLE_FONT_PICK,
               FontManager.getFontLinks(),
               l -> this.editor.editMultiple(((Lyrics3DClip)this.clip).font, v -> v.set(l))
            );
            UIOverlay.addOverlay(this.getContext(), panel.set((Link)((Lyrics3DClip)this.clip).font.get()));
         }
      );
      this.pickFont.tooltip(BBSFxKeys.L3D_FONT_HINT);
      this.color = this.color(((Lyrics3DClip)this.clip).color).withAlpha();
      this.gradient = this.toggle(BBSFxKeys.GRADIENT, ((Lyrics3DClip)this.clip).gradient);
      this.gradientFrom = this.color(((Lyrics3DClip)this.clip).gradientFrom).withAlpha();
      this.gradientTo = this.color(((Lyrics3DClip)this.clip).gradientTo).withAlpha();
      this.gradientMode = this.cycler(new IKey[]{BBSFxKeys.GRADIENT_HORIZONTAL, BBSFxKeys.GRADIENT_VERTICAL}, ((Lyrics3DClip)this.clip).gradientMode);
      this.textShadow = this.toggle(BBSFxKeys.L3D_TEXT_SHADOW, ((Lyrics3DClip)this.clip).textShadow);
      this.maxWidth = this.number(((Lyrics3DClip)this.clip).maxWidth, BBSFxKeys.L3D_MAX_WIDTH).forcedLabel(BBSFxKeys.L3D_MAX_WIDTH_SHORT);
      this.glow = this.toggle(BBSFxKeys.L3D_GLOW, ((Lyrics3DClip)this.clip).glow);
      this.glow.tooltip(BBSFxKeys.L3D_GLOW_HINT);
      this.glowColor = this.color(((Lyrics3DClip)this.clip).glowColor);
      this.glowStrength = this.number(((Lyrics3DClip)this.clip).glowStrength, BBSFxKeys.L3D_GLOW_STRENGTH).forcedLabel(BBSFxKeys.L3D_GLOW_STRENGTH_SHORT);
      this.glowRadius = this.number(((Lyrics3DClip)this.clip).glowRadius, BBSFxKeys.L3D_GLOW_RADIUS).forcedLabel(BBSFxKeys.L3D_GLOW_RADIUS_SHORT);
      this.wiggle = this.toggle(BBSFxKeys.L3D_WIGGLE, ((Lyrics3DClip)this.clip).wiggle);
      this.wiggle.tooltip(BBSFxKeys.L3D_WIGGLE_HINT);
      this.wiggleFps = this.number(((Lyrics3DClip)this.clip).wiggleFps, BBSFxKeys.L3D_WIGGLE_FPS).forcedLabel(BBSFxKeys.L3D_WIGGLE_FPS_SHORT);
      this.wiggleAmount = this.number(((Lyrics3DClip)this.clip).wiggleAmount, BBSFxKeys.L3D_WIGGLE_AMOUNT).forcedLabel(BBSFxKeys.L3D_WIGGLE_AMOUNT_SHORT);
      this.wordPop = this.toggle(BBSFxKeys.L3D_WORD_POP, ((Lyrics3DClip)this.clip).wordPop);
      this.beat = this.number(((Lyrics3DClip)this.clip).beat, BBSFxKeys.L3D_BEAT).forcedLabel(BBSFxKeys.L3D_BEAT_SHORT);
   }

   @Override
   protected void registerPanels() {
      super.registerPanels();
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.L3D_PLACEMENT,
               BBSFxKeys.L3D_HINT,
               UI.row(new UIElement[]{this.distance, this.spread}),
               UI.row(new UIElement[]{this.drop, this.scale}),
               UI.row(new UIElement[]{this.positionX, this.positionY}),
               this.positionZ,
               UI.row(new UIElement[]{this.yawOffset, this.pitchOffset}),
               this.roll,
               UI.row(new UIElement[]{this.faceCamera, this.keepAway}),
               UI.row(new UIElement[]{this.aboveGround, this.throughWalls}),
               this.followCamera,
               this.pickActor
            )
         );
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.L3D_TEXT,
               BBSFxKeys.L3D_TEXT_HINT,
               UI.labelRow(BBSFxKeys.L3D_COLOR, this.color),
               UI.row(new UIElement[]{this.gradient, this.gradientMode}),
               UI.row(new UIElement[]{this.gradientFrom, this.gradientTo}),
               UI.row(new UIElement[]{this.textShadow, this.smoothFont}),
               UI.labelRow(BBSFxKeys.L3D_FONT_STYLE, this.fontStyle),
               this.pickFont,
               UI.row(new UIElement[]{this.uppercase, this.bilingual}),
               this.translationRow = UI.row(new UIElement[]{this.pickTranslation, this.swapTranslation}),
               this.translationStatus,
               UI.labelRow(BBSFxKeys.L3D_ALIGN, this.align),
               this.maxWidth,
               UI.labelRow(BBSFxKeys.L3D_INTRO, this.intro),
               UI.labelRow(BBSFxKeys.L3D_CHAR_FX, this.charFx)
            )
         );
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.L3D_GLOW_SECTION,
               BBSFxKeys.L3D_GLOW_HINT,
               this.glow,
               UI.labelRow(BBSFxKeys.L3D_GLOW_COLOR, this.glowColor),
               UI.row(
                  new UIElement[]{
                     this.glowPreset(BBSFxKeys.L3D_GLOW_TEXT, GLOW_PRESETS[0]),
                     this.glowPreset(BBSFxKeys.L3D_GLOW_PINK, GLOW_PRESETS[1]),
                     this.glowPreset(BBSFxKeys.L3D_GLOW_BLUE, GLOW_PRESETS[2]),
                     this.glowPreset(BBSFxKeys.L3D_GLOW_GOLD, GLOW_PRESETS[3])
                  }
               ),
               UI.row(new UIElement[]{this.glowStrength, this.glowRadius})
            )
         );
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.L3D_MOTION,
               BBSFxKeys.L3D_MOTION_HINT,
               UI.row(new UIElement[]{this.wiggle, this.wordPop}),
               UI.row(new UIElement[]{this.wiggleFps, this.wiggleAmount}),
               this.beat
            )
         );
      this.refreshActorLabel();
      this.refreshTranslationControls();
   }

   public void fillData() {
      super.fillData();
      this.refreshActorLabel();
      this.refreshTranslationControls();
   }

   private void refreshTranslationControls() {
      boolean segmented = (Boolean)((Lyrics3DClip)this.clip).segmentMode.get();
      this.translationRow.setVisible(!segmented);
      this.translationStatus.setVisible(!segmented);
      Link explicit = (Link)((Lyrics3DClip)this.clip).translation.get();
      this.pickTranslation.label = explicit == null
         ? BBSFxKeys.MUSIC_TRANSLATION_BUTTON_AUTO
         : BBSFxKeys.MUSIC_TRANSLATION_BUTTON_FILE.format(new Object[]{shortName(explicit)});
      UIMusicTextClip.ResolvedLyrics resolved = this.resolveLyrics();
      Lrc data = ((Lyrics3DClip)this.clip).effectiveLyrics(resolved.link(), resolved.data());
      int total = 0;
      int translated = 0;
      if (data != null) {
         for (Lrc.Line line : LyricSplit.view(data, (Boolean)((Lyrics3DClip)this.clip).splitLines.get())) {
            if (!line.text.isBlank()) {
               total++;
               if (line.translation != null) {
                  translated++;
               }
            }
         }
      }

      this.translationStatus.label = translated > 0
         ? BBSFxKeys.MUSIC_TRANSLATION_STATUS.format(new Object[]{translated, total})
         : BBSFxKeys.MUSIC_TRANSLATION_NONE;
      String source;
      if (translated <= 0) {
         source = BBSFxKeys.MUSIC_TRANSLATION_HINT.get();
      } else if (explicit != null) {
         source = BBSFxKeys.MUSIC_TRANSLATION_SOURCE_MANUAL.format(new Object[]{shortName(explicit)}).get();
      } else {
         Link sidecar = resolved.link() == null ? null : Lrc.findTranslation(resolved.link());
         source = sidecar != null
            ? BBSFxKeys.MUSIC_TRANSLATION_SOURCE_AUTO.format(new Object[]{shortName(sidecar)}).get()
            : BBSFxKeys.MUSIC_TRANSLATION_SOURCE_INLINE.get();
      }

      this.translationStatus.tooltip(IKey.raw(source), Direction.BOTTOM);
   }

   private void openTranslationPicker() {
      Link main = ((Lyrics3DClip)this.clip).lrc.get() != null ? (Link)((Lyrics3DClip)this.clip).lrc.get() : this.resolveLyrics().link();
      List<Link> links = new ArrayList();

      for (Link link : this.lyricsLinks()) {
         if (!link.equals(main)) {
            links.add(link);
         }
      }

      UIStringOverlayPanel panel = UIStringOverlayPanel.links(BBSFxKeys.MUSIC_TRANSLATION_PICK, links, linkx -> {
         this.editor.editMultiple(((Lyrics3DClip)this.clip).translation, v -> v.set(linkx));
         this.refreshTranslationControls();
      });
      UIOverlay.addOverlay(this.getContext(), panel.set((Link)((Lyrics3DClip)this.clip).translation.get()));
   }

   private static String shortName(Link link) {
      if (link != null && link.path != null) {
         String name = link.path;
         int slash = Math.max(name.lastIndexOf(47), name.lastIndexOf(92));
         return slash >= 0 ? name.substring(slash + 1) : name;
      } else {
         return "";
      }
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

      String current = (String)((Lyrics3DClip)this.clip).actor.get();
      int index = ids.indexOf(current);
      String next = (String)ids.get((index + 1) % ids.size());
      this.editor.editMultiple(((Lyrics3DClip)this.clip).actor, v -> v.set(next));
      this.refreshActorLabel();
   }

   private void refreshActorLabel() {
      String id = (String)((Lyrics3DClip)this.clip).actor.get();
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

   private UICirculate cycler(IKey[] labels, ValueInt value) {
      UICirculate circulate = new UICirculate(null);

      for (IKey label : labels) {
         circulate.addLabel(label);
      }

      circulate.callback = c -> this.editor.editMultiple(value, v -> v.set(c.getValue()));
      circulate.setValue((Integer)value.get());
      circulate.w(90);
      this.bind(circulate, () -> circulate.setValue((Integer)value.get()));
      return circulate;
   }

   private UIButton glowPreset(IKey label, int color) {
      return new UIButton(label, b -> {
         this.editor.editMultiple(((Lyrics3DClip)this.clip).glowColor, v -> v.set(color));
         this.glowColor.setColor(color);
      });
   }

   @Environment(EnvType.CLIENT)
   private class TranslationButton extends UIButton {
      TranslationButton() {
         super(BBSFxKeys.MUSIC_TRANSLATION, b -> UILyrics3DClip.this.openTranslationPicker());
         this.tooltip(BBSFxKeys.MUSIC_TRANSLATION_HINT, Direction.BOTTOM);
      }

      public boolean subMouseClicked(UIContext context) {
         if (context.mouseButton == 1) {
            UILyrics3DClip.this.editor.editMultiple(((Lyrics3DClip)UILyrics3DClip.this.clip).translation, v -> v.set(null));
            UILyrics3DClip.this.refreshTranslationControls();
            return true;
         } else {
            return super.subMouseClicked(context);
         }
      }
   }
}
