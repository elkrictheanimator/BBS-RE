package bbsfx.client.ui;

import bbsfx.client.lyrics.BeatCache;
import bbsfx.lyrics.Lrc;
import bbsfx.lyrics.LyricPhrases;
import bbsfx.lyrics.LyricSplit;
import bbsfx.lyrics.LyricTrackSplitter;
import bbsfx.lyrics.MusicTextClip;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UIClip;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.UISection;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UICirculate;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIMessageOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UISoundOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UITextareaOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.utils.Direction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public abstract class UIMusicTextClip<T extends MusicTextClip> extends UIClip<T> {
   protected UIButton pickLrc;
   protected UIButton pickAudio;
   protected UITrackpad offset;
   protected UIToggle splitLines;
   protected UITrackpad seed;
   protected UIButton splitTrack;
   protected UITextbox segmentText;
   protected UIToggle phraseMode;
   protected UIButton editPhrases;
   protected UICirculate phraseTemplate;
   protected UITrackpad phraseDuration;
   protected UIToggle phraseLoop;
   protected UIElement segmentRow;
   protected UIElement phraseTemplateRow;
   protected UIElement phraseDurationRow;

   public UIMusicTextClip(T clip, IUIClipsDelegate editor) {
      super(clip, editor);
      this.markContainer();
   }

   public boolean subMouseClicked(UIContext context) {
      return super.subMouseClicked(context) || this.area.isInside(context);
   }

   public boolean subMouseReleased(UIContext context) {
      return super.subMouseReleased(context) || this.area.isInside(context);
   }

   protected void registerUI() {
      super.registerUI();
      Lrc.clearCache();
      LyricSplit.clearCache();
      BeatCache.clear();
      this.pickLrc = new UIButton(BBSFxKeys.LYRICS_PICK, b -> this.openLrcPicker());
      this.pickLrc.tooltip(BBSFxKeys.MUSIC_HINT);
      this.pickAudio = new UIButton(
         BBSFxKeys.LYRICS_AUDIO,
         b -> {
            UISoundOverlayPanel panel = new UISoundOverlayPanel(
               link -> this.editor.editMultiple(((MusicTextClip)this.clip).audio, v -> v.set(link)), this.getContext()
            );
            UIOverlay.addOverlay(this.getContext(), panel.set((Link)((MusicTextClip)this.clip).audio.get()));
         }
      );
      this.pickAudio.tooltip(BBSFxKeys.MUSIC_HINT);
      this.offset = this.number(((MusicTextClip)this.clip).offset, BBSFxKeys.LYRICS_OFFSET).forcedLabel(BBSFxKeys.LYRICS_OFFSET_SHORT);
      this.seed = this.number(((MusicTextClip)this.clip).seed, BBSFxKeys.MUSIC_SEED).forcedLabel(BBSFxKeys.MUSIC_SEED_SHORT);
      this.splitLines = this.toggle(BBSFxKeys.MUSIC_SPLIT, ((MusicTextClip)this.clip).splitLines);
      this.splitLines.tooltip(BBSFxKeys.MUSIC_SPLIT_HINT);
      this.splitTrack = new UIButton(BBSFxKeys.MUSIC_TRACK_SPLIT, b -> this.splitTrack());
      this.splitTrack.tooltip(BBSFxKeys.MUSIC_TRACK_SPLIT_HINT);
      this.segmentText = new UITextbox(text -> this.editor.editMultiple(((MusicTextClip)this.clip).segmentText, v -> v.set(text)));
      this.segmentText.setText((String)((MusicTextClip)this.clip).segmentText.get());
      this.segmentText.tooltip(BBSFxKeys.MUSIC_SEGMENT_HINT, Direction.BOTTOM);
      this.bind(this.segmentText, () -> {
         this.segmentText.setText((String)((MusicTextClip)this.clip).segmentText.get());
         this.refreshPhraseControls();
      });
      this.phraseMode = new UIToggle(BBSFxKeys.MUSIC_PHRASE_MODE, (Boolean)((MusicTextClip)this.clip).phraseMode.get(), b -> {
         this.editor.editMultiple(((MusicTextClip)this.clip).phraseMode, v -> v.set(b.getValue()));
         this.refreshPhraseControls();
      });
      this.phraseMode.tooltip(BBSFxKeys.MUSIC_PHRASE_HINT);
      this.bind(this.phraseMode, () -> {
         this.phraseMode.setValue((Boolean)((MusicTextClip)this.clip).phraseMode.get());
         this.refreshPhraseControls();
      });
      this.editPhrases = new UIButton(BBSFxKeys.MUSIC_PHRASE_EDIT, b -> this.openPhraseEditor());
      this.editPhrases.tooltip(BBSFxKeys.MUSIC_PHRASE_EDIT_HINT);
      this.phraseTemplate = this.modeCycler(
         new IKey[]{BBSFxKeys.MUSIC_PHRASE_POP, BBSFxKeys.MUSIC_PHRASE_FADE, BBSFxKeys.MUSIC_PHRASE_RISE}, ((MusicTextClip)this.clip).phraseTemplate
      );
      this.phraseTemplate.tooltip(BBSFxKeys.MUSIC_PHRASE_HINT);
      this.phraseDuration = this.number(((MusicTextClip)this.clip).phraseDuration, BBSFxKeys.MUSIC_PHRASE_DURATION_HINT)
         .forcedLabel(BBSFxKeys.MUSIC_PHRASE_DURATION);
      this.phraseLoop = this.toggle(BBSFxKeys.MUSIC_PHRASE_LOOP, ((MusicTextClip)this.clip).phraseLoop);
      this.phraseLoop.tooltip(BBSFxKeys.MUSIC_PHRASE_HINT);
   }

   protected void registerPanels() {
      super.registerPanels();
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.MUSIC_SOURCE,
               BBSFxKeys.MUSIC_HINT,
               UI.row(new UIElement[]{this.pickLrc, this.pickAudio}),
               UI.row(new UIElement[]{this.offset, this.seed}),
               this.splitLines
            )
         );
      this.segmentRow = UI.labelRow(BBSFxKeys.MUSIC_SEGMENT_TEXT, this.segmentText);
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.MUSIC_TRACK_SECTION,
               ((MusicTextClip)this.clip).segmentMode.get() ? BBSFxKeys.MUSIC_SEGMENT_HINT : BBSFxKeys.MUSIC_TRACK_SPLIT_HINT,
               this.splitTrack,
               this.segmentRow
            )
         );
      this.phraseTemplateRow = UI.labelRow(BBSFxKeys.MUSIC_PHRASE_TEMPLATE, this.phraseTemplate);
      this.phraseDurationRow = this.phraseDuration;
      this.panels
         .add(
            this.helpSection(
               BBSFxKeys.MUSIC_PHRASES,
               BBSFxKeys.MUSIC_PHRASE_HINT,
               this.phraseMode,
               this.editPhrases,
               this.phraseTemplateRow,
               this.phraseDurationRow,
               this.phraseLoop
            )
         );
      this.refreshPhraseControls();
   }

   protected UISection helpSection(IKey title, IKey help, UIElement... elements) {
      UISection section = this.section(title, elements);
      section.title.tooltip(help, Direction.BOTTOM);
      return section;
   }

   private void refreshPhraseControls() {
      boolean segmented = (Boolean)((MusicTextClip)this.clip).segmentMode.get();
      boolean enabled = (Boolean)((MusicTextClip)this.clip).phraseMode.get() && !segmented;
      this.splitTrack.setEnabled(!segmented);
      this.splitTrack.setVisible(!segmented);
      this.segmentText.setEnabled(segmented);
      this.segmentRow.setVisible(segmented);
      this.phraseMode.setEnabled(!segmented);
      this.phraseMode.setVisible(!segmented);
      this.editPhrases.setEnabled(!segmented);
      this.editPhrases.setVisible(!segmented);
      this.phraseTemplate.setEnabled((Boolean)((MusicTextClip)this.clip).phraseMode.get());
      this.phraseTemplateRow.setVisible(!segmented);
      this.phraseDuration.setEnabled(enabled);
      this.phraseDurationRow.setVisible(enabled);
      this.phraseLoop.setEnabled(enabled);
      this.phraseLoop.setVisible(enabled);
   }

   private void openPhraseEditor() {
      String current = (String)((MusicTextClip)this.clip).phrases.get();
      String initial = current != null && !current.isBlank() ? current : this.importLyrics();
      UITextareaOverlayPanel panel = new UITextareaOverlayPanel(
         BBSFxKeys.MUSIC_PHRASE_EDIT_TITLE,
         BBSFxKeys.MUSIC_PHRASE_EDIT_HINT,
         text -> this.editor.editMultiple(((MusicTextClip)this.clip).phrases, v -> v.set(text))
      );
      panel.text.setText(initial);
      UIOverlay.addOverlay(this.getContext(), panel, 520, 320);
   }

   private void splitTrack() {
      if (!(Boolean)((MusicTextClip)this.clip).segmentMode.get()) {
         Film film = this.editor.getFilm();
         UIMusicTextClip.ResolvedLyrics resolved = this.resolveLyrics();
         Lrc effective = ((MusicTextClip)this.clip).effectiveLyrics(resolved.link(), resolved.data());
         boolean manual = (Boolean)((MusicTextClip)this.clip).phraseMode.get()
            && !LyricPhrases.parse((String)((MusicTextClip)this.clip).phrases.get()).isEmpty();
         List<Lrc.Line> lines = manual
            ? LyricPhrases.view(
               (String)((MusicTextClip)this.clip).phrases.get(), List.of(), 0.0F, (Float)((MusicTextClip)this.clip).phraseDuration.get() * 1000.0F, false
            )
            : (effective == null ? List.of() : LyricSplit.view(effective, (Boolean)((MusicTextClip)this.clip).splitLines.get()));
         long tail = ((MusicTextClip)this.clip).phraseMode.get() ? Math.round((Float)((MusicTextClip)this.clip).phraseDuration.get() * 1000.0F) : 5000L;
         List<LyricTrackSplitter.Segment> segments = LyricTrackSplitter.split((MusicTextClip)this.clip, lines, resolved.audio, tail);
         if (film != null && !segments.isEmpty()) {
            List<MusicTextClip> copies = new ArrayList();

            for (int i = 1; i < segments.size(); i++) {
               copies.add((MusicTextClip)((MusicTextClip)this.clip).copy());
            }

            LyricTrackSplitter.assign((MusicTextClip)this.clip, (LyricTrackSplitter.Segment)segments.get(0));

            for (int i = 0; i < copies.size(); i++) {
               MusicTextClip part = (MusicTextClip)copies.get(i);
               LyricTrackSplitter.assign(part, (LyricTrackSplitter.Segment)segments.get(i + 1));
               film.camera.addClip(part);
            }

            film.camera.sortLayers();
            film.camera.sync();
            this.editor.markLastUndoNoMerging();
            this.editor.fillData();
            this.message(BBSFxKeys.MUSIC_TRACK_SPLIT_DONE.format(new Object[]{segments.size()}));
         } else {
            this.message(BBSFxKeys.MUSIC_TRACK_SPLIT_EMPTY);
         }
      }
   }

   private void message(IKey message) {
      UIOverlay.addOverlay(this.getContext(), new UIMessageOverlayPanel(BBSFxKeys.MUSIC_TRACK_SPLIT, message));
   }

   private String importLyrics() {
      UIMusicTextClip.ResolvedLyrics resolved = this.resolveLyrics();
      Lrc data = ((MusicTextClip)this.clip).effectiveLyrics(resolved.link(), resolved.data());
      return data == null ? "" : LyricPhrases.join(LyricSplit.view(data, (Boolean)((MusicTextClip)this.clip).splitLines.get()));
   }

   protected UIMusicTextClip.ResolvedLyrics resolveLyrics() {
      Film film = this.editor.getFilm();
      List<AudioClip> clips = film == null ? List.of() : film.camera.getClips(AudioClip.class);
      Link wanted = (Link)((MusicTextClip)this.clip).audio.get();
      AudioClip active = null;
      AudioClip fallback = null;

      for (AudioClip audio : clips) {
         if ((Boolean)audio.enabled.get() && (wanted == null || wanted.equals(audio.audio.get()))) {
            if (fallback == null) {
               fallback = audio;
            }

            int tick = (Integer)((MusicTextClip)this.clip).tick.get();
            if (tick >= (Integer)audio.tick.get() && tick < (Integer)audio.tick.get() + (Integer)audio.duration.get()) {
               active = audio;
               break;
            }
         }
      }

      AudioClip source = active == null ? fallback : active;
      Lrc data = Lrc.get((Link)((MusicTextClip)this.clip).lrc.get());
      Link link = (Link)((MusicTextClip)this.clip).lrc.get();
      if (data == null && wanted != null) {
         link = Lrc.find(wanted);
         data = this.lyricsOf(wanted);
      }

      if (data == null && source != null) {
         link = Lrc.find((Link)source.audio.get());
         data = this.lyricsOf((Link)source.audio.get());
      }

      if (data == null && wanted == null) {
         for (AudioClip audio : clips) {
            if ((Boolean)audio.enabled.get() && audio != source) {
               data = this.lyricsOf((Link)audio.audio.get());
               if (data != null) {
                  link = Lrc.find((Link)audio.audio.get());
                  source = audio;
                  break;
               }
            }
         }
      }

      return new UIMusicTextClip.ResolvedLyrics(data, source, link);
   }

   private Lrc lyricsOf(Link audio) {
      return audio == null ? null : Lrc.get(Lrc.find(audio));
   }

   protected UICirculate modeCycler(IKey[] labels, ValueInt value) {
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

   protected UITrackpad number(ValueFloat value, IKey tooltip) {
      UISliderTrackpad trackpad = new UISliderTrackpad(v -> this.editor.editMultiple(value, f -> f.set(v.floatValue())), tooltip);
      trackpad.limit(value);
      trackpad.tooltip(tooltip, Direction.BOTTOM);
      this.bind(trackpad, () -> trackpad.setValue(((Float)value.get()).floatValue()));
      return trackpad;
   }

   protected UITrackpad number(ValueInt value, IKey tooltip) {
      UISliderTrackpad trackpad = new UISliderTrackpad(v -> this.editor.editMultiple(value, i -> i.set(v.intValue())), tooltip);
      trackpad.integer().limit(value);
      trackpad.tooltip(tooltip, Direction.BOTTOM);
      this.bind(trackpad, () -> trackpad.setValue(((Integer)value.get()).intValue()));
      return trackpad;
   }

   protected List<Link> lyricsLinks() {
      List<Link> links = new ArrayList();

      for (String folder : new String[]{"lyrics", "audio"}) {
         try {
            for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets(folder), true)) {
               if (Lrc.isLyrics(link.path)) {
                  links.add(link);
               }
            }
         } catch (Exception var8) {
         }
      }

      return links;
   }

   private void openLrcPicker() {
      UIStringOverlayPanel panel = UIStringOverlayPanel.links(
         BBSFxKeys.LYRICS_PICK, this.lyricsLinks(), link -> this.editor.editMultiple(((MusicTextClip)this.clip).lrc, v -> v.set(link))
      );
      UIOverlay.addOverlay(this.getContext(), panel.set((Link)((MusicTextClip)this.clip).lrc.get()));
   }

   @Environment(EnvType.CLIENT)
   protected record ResolvedLyrics(Lrc data, AudioClip audio, Link link) {
   }
}
