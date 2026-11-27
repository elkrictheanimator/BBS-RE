package bbsfx.lyrics;

import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.clips.misc.OverlayBox;
import mchorse.bbs_mod.camera.clips.misc.Subtitle;
import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.camera.data.Placement;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueColor;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;

public class LyricsClip extends SubtitleClip {
   public static final int STYLE_SLAM = 0;
   public static final int STYLE_FLASH = 1;
   public static final int STYLE_STATIC = 2;
   public ValueLink lrc = new ValueLink("lrc", null);
   public ValueLink audio = new ValueLink("audio", null);
   public ValueFloat offset = new ValueFloat("offset", 0.0F, -60000.0F, 60000.0F);
   public ValueInt style = new ValueInt("style", 0, 0, 2);
   public ValueFloat pop = new ValueFloat("pop", 1.0F, 0.0F, 3.0F);
   public ValueFloat hold = new ValueFloat("hold", 7.0F, 0.5F, 60.0F);
   public ValueColor highlight = new ValueColor("highlight", new Color(1.0F, 0.35F, 0.65F, 1.0F));
   protected final Subtitle overlay = new Subtitle();

   public LyricsClip() {
      this.add(this.lrc);
      this.add(this.audio);
      this.add(this.offset);
      this.add(this.style);
      this.add(this.pop);
      this.add(this.hold);
      this.add(this.highlight);
   }

   protected Clip create() {
      return new LyricsClip();
   }

   public OverlayBox getOverlayBox() {
      return this.overlay.box;
   }

   public static float songTicks(AudioClip clip, int ticks, float transition) {
      return LyricClock.songTicks(clip, ticks, transition);
   }

   protected AudioClip sourceAudio(ClipContext context) {
      return LyricClock.sourceAudio(context, (Link)this.audio.get());
   }

   protected float lyricMillis(ClipContext context) {
      return LyricClock.millis(context, this.sourceAudio(context), (Float)this.offset.get());
   }

   protected Link lyricLink(ClipContext context) {
      return LyricClock.lyricLink(context, (Link)this.lrc.get(), (Link)this.audio.get());
   }

   protected Lrc.Line lyricLine(ClipContext context) {
      Lrc data = Lrc.get(this.lyricLink(context));
      return data == null ? null : data.at(this.lyricMillis(context));
   }

   protected float envelopeFactor(ClipContext context) {
      return this.envelope.factorEnabled((Integer)this.duration.get(), context.relativeTick + context.transition);
   }

   protected void configure(Subtitle subtitle, String text, Placement placement, int color, float factor) {
      subtitle.update(text, placement, Colors.setA(color, factor * Colors.getA(color)), (Boolean)this.textShadow.get());
      subtitle.updateBackground((Integer)this.background.get(), (Float)this.backgroundOffset.get(), (Float)this.shadow.get(), (Boolean)this.shadowOpaque.get());
      subtitle.updateTransform((Transform)this.transform.get(), factor);
      subtitle.updateConstraints((Integer)this.lineHeight.get(), (Integer)this.maxWidth.get());
      subtitle.updateFont((Link)this.font.get(), (Integer)this.fontSize.get());
      subtitle.updateImage((Link)this.image.get(), (Boolean)this.imageRight.get(), (Float)this.imageScale.get());
   }

   protected void applyClip(ClipContext context, Position position) {
      Lrc.Line line = this.lyricLine(context);
      if (line != null && !line.text.isEmpty()) {
         float millis = this.lyricMillis(context);
         float age = (millis - (float)line.time) / 1000.0F;
         if (!(age > (Float)this.hold.get())) {
            float settle = Math.min(1.0F, age / 0.32F);
            float hit = this.style.get() == 2 ? 0.0F : (1.0F - settle) * (Float)this.pop.get();
            float scale = 1.0F + hit;
            Placement placement = ((Placement)this.placement.get()).copy();
            placement.scaleX *= scale;
            placement.scaleY *= scale;
            int color = (Integer)this.color.get();
            if ((Integer)this.style.get() == 1 && age < 0.16F) {
               color = ((Color)this.highlight.get()).getARGBColor();
            }

            this.configure(this.overlay, line.text, placement, color, this.envelopeFactor(context));
            getSubtitles(context).add(this.overlay);
         }
      }
   }
}
