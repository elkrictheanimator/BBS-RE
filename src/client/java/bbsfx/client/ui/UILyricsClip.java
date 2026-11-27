package bbsfx.client.ui;

import bbsfx.lyrics.Lrc;
import bbsfx.lyrics.LyricsClip;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.L10n;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.clips.UISubtitleClip;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UISoundOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.values.UIValues;
import mchorse.bbs_mod.utils.Direction;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class UILyricsClip extends UISubtitleClip {
   private UIButton pickLrc;
   private UIButton pickAudio;
   private UIButton style;
   private UITrackpad offset;
   private UITrackpad pop;
   private UITrackpad hold;
   private UIColor highlight;

   public UILyricsClip(LyricsClip clip, IUIClipsDelegate editor) {
      super(clip, editor);
   }

   protected LyricsClip lyrics() {
      return (LyricsClip)this.clip;
   }

   protected void registerUI() {
      super.registerUI();
      Lrc.clearCache();
      this.pickLrc = new UIButton(BBSFxKeys.LYRICS_PICK, b -> this.openLrcPicker());
      this.pickAudio = new UIButton(BBSFxKeys.LYRICS_AUDIO, b -> {
         UISoundOverlayPanel panel = new UISoundOverlayPanel(link -> {
            this.editor.editMultiple(this.lyrics().audio, v -> v.set(link));
            Lrc.clearCache();
            Link found = Lrc.find(link);
            if (found != null) {
               this.editor.editMultiple(this.lyrics().lrc, v -> v.set(found));
            }
         }, this.getContext());
         UIOverlay.addOverlay(this.getContext(), panel.set((Link)this.lyrics().audio.get()));
      });
      this.style = new UIButton(BBSFxKeys.LYRICS_STYLE, b -> {
         int next = ((Integer)this.lyrics().style.get() + 1) % 3;
         this.editor.editMultiple(this.lyrics().style, next);
         this.updateStyleLabel();
      });
      this.offset = this.trackpad(this.lyrics().offset).limit(-60000.0, 60000.0);
      this.offset.tooltip(BBSFxKeys.LYRICS_OFFSET, Direction.BOTTOM);
      this.pop = this.trackpad(this.lyrics().pop).limit(0.0, 3.0);
      this.pop.tooltip(BBSFxKeys.LYRICS_POP, Direction.BOTTOM);
      this.hold = this.trackpad(this.lyrics().hold).limit(0.5, 60.0);
      this.hold.tooltip(BBSFxKeys.LYRICS_HOLD, Direction.BOTTOM);
      this.highlight = UIValues.color(() -> this.lyrics().highlight).withAlpha();
      this.updateStyleLabel();
   }

   protected void registerPanels() {
      super.registerPanels();
      this.panels
         .add(
            this.section(
               BBSFxKeys.LYRICS_SECTION,
               new UIElement[]{
                  UI.row(new UIElement[]{this.pickLrc, this.style}),
                  UI.row(new UIElement[]{this.pickAudio, this.offset}),
                  UI.row(new UIElement[]{this.pop, this.hold}),
                  this.highlight,
                  BBSFxKeys.hint(BBSFxKeys.LYRICS_HINT)
               }
            )
         );
   }

   public void fillData() {
      super.fillData();
      this.updateStyleLabel();
   }

   private void updateStyleLabel() {
      String key = switch (this.lyrics().style.get()) {
         case 1 -> "bbsfx.ui.lyrics.style.flash";
         case 2 -> "bbsfx.ui.lyrics.style.static";
         default -> "bbsfx.ui.lyrics.style.slam";
      };
      this.style.label = L10n.lang(key);
   }

   private void openLrcPicker() {
      List<Link> links = new ArrayList();
      Lrc.clearCache();

      try {
         for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("lyrics"), true)) {
            if (link.toString().toLowerCase().endsWith(".lrc")) {
               links.add(link);
            }
         }
      } catch (Exception var4) {
      }

      UIStringOverlayPanel panel = UIStringOverlayPanel.links(
         BBSFxKeys.LYRICS_PICK, links, linkx -> this.editor.editMultiple(this.lyrics().lrc, v -> v.set(linkx))
      );
      UIOverlay.addOverlay(this.getContext(), panel.set((Link)this.lyrics().lrc.get()));
   }
}
