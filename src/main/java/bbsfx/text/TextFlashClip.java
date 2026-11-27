package bbsfx.text;

import bbsfx.lyrics.Lrc;
import bbsfx.lyrics.LyricSplit;
import bbsfx.lyrics.MusicTextClip;
import java.util.ArrayList;
import java.util.List;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

public class TextFlashClip extends MusicTextClip {
   public static final String FRAMES = "bbsfx:textflash";
   public final ValueFloat size = new ValueFloat("size", 1.0F, 0.5F, 2.0F);
   public final ValueFloat x = new ValueFloat("x", 0.5F, 0.0F, 1.0F);
   public final ValueFloat y = new ValueFloat("y", 0.46F, 0.0F, 1.0F);
   public final ValueBoolean uppercase = new ValueBoolean("uppercase", true);
   public final ValueLink font = new ValueLink("font", null);
   public final ValueInt white = new ValueInt("white", -1);
   public final ValueBoolean gradient = new ValueBoolean("gradient", false);
   public final ValueInt gradientFrom = new ValueInt("gradientFrom", -1);
   public final ValueInt gradientTo = new ValueInt("gradientTo", -5737473);
   public final ValueInt gradientMode = new ValueInt("gradientMode", 0, 0, 1);
   public final ValueInt red = new ValueInt("red", -54444);
   public final ValueFloat redChance = new ValueFloat("redChance", 0.32F, 0.0F, 1.0F);
   public final ValueFloat glowChance = new ValueFloat("glowChance", 0.35F, 0.0F, 1.0F);
   public final ValueFloat glowStrength = new ValueFloat("glowStrength", 1.0F, 0.0F, 3.0F);
   public final ValueFloat slamChance = new ValueFloat("slamChance", 0.22F, 0.0F, 1.0F);
   public final ValueFloat tilt = new ValueFloat("tilt", 12.0F, 0.0F, 45.0F);
   public final ValueInt rowLength = new ValueInt("rowLength", 8, 4, 20);
   public final ValueInt layout = new ValueInt("layout", 0, 0, 2);
   public final ValueFloat tracking = new ValueFloat("tracking", 0.0F, -0.2F, 1.0F);
   public final ValueFloat lineHeight = new ValueFloat("lineHeight", 0.82F, 0.4F, 2.0F);
   public final ValueInt outline = new ValueInt("outline", 0);
   public final ValueFloat outlineWidth = new ValueFloat("outlineWidth", 1.0F, 0.0F, 5.0F);
   public final ValueFloat beat = new ValueFloat("beat", 1.0F, 0.0F, 2.0F);
   public final ValueString actor = new ValueString("actor", "");
   public final ValueFloat avoidMargin = new ValueFloat("avoidMargin", 0.04F, 0.0F, 0.3F);
   public final ValueFloat beatSpin = new ValueFloat("beatSpin", 0.0F, 0.0F, 30.0F);
   public final ValueFloat beatFlash = new ValueFloat("beatFlash", 0.0F, 0.0F, 1.0F);
   public final ValueFloat wave = new ValueFloat("wave", 0.0F, 0.0F, 1.0F);
   public final ValueFloat waveSpeed = new ValueFloat("waveSpeed", 1.0F, 0.0F, 4.0F);
   public final ValueFloat strobe = new ValueFloat("strobe", 0.0F, 0.0F, 1.0F);

   public TextFlashClip() {
      this.add(this.size);
      this.add(this.x);
      this.add(this.y);
      this.add(this.uppercase);
      this.add(this.font);
      this.add(this.white);
      this.add(this.gradient);
      this.add(this.gradientFrom);
      this.add(this.gradientTo);
      this.add(this.gradientMode);
      this.add(this.red);
      this.add(this.redChance);
      this.add(this.glowChance);
      this.add(this.glowStrength);
      this.add(this.slamChance);
      this.add(this.tilt);
      this.add(this.rowLength);
      this.add(this.layout);
      this.add(this.tracking);
      this.add(this.lineHeight);
      this.add(this.outline);
      this.add(this.outlineWidth);
      this.add(this.beat);
      this.add(this.actor);
      this.add(this.avoidMargin);
      this.add(this.beatSpin);
      this.add(this.beatFlash);
      this.add(this.wave);
      this.add(this.waveSpeed);
      this.add(this.strobe);
   }

   public static List<TextFlashClip.Frame> getFrames(ClipContext context) {
      return context.clipData.get("bbsfx:textflash", () -> new ArrayList<TextFlashClip.Frame>());
   }

   public TextFlashLayout.Style style() {
      TextFlashLayout.Style style = new TextFlashLayout.Style();
      style.slamChance = (Float)this.slamChance.get();
      style.redChance = (Float)this.redChance.get();
      style.glowChance = (Float)this.glowChance.get();
      style.tilt = (Float)this.tilt.get();
      style.rowLength = (Integer)this.rowLength.get();
      style.uppercase = (Boolean)this.uppercase.get();
      style.seed = (Integer)this.seed.get();
      return style;
   }

   @Override
   protected void collect(ClipContext context, Position position, List<Lrc.Line> lines, float now) {
      float envelope = this.envelopeFactor(context);
      if (!(envelope <= 0.0F)) {
         int index = LyricSplit.indexAt(lines, now);
         List<TextFlashClip.Frame> frames = getFrames(context);
         TextFlashLayout.Style style = this.style();
         Link beatAudio = this.beatAudio(context);
         float beatMillis = this.beatMillis(context);
         float clock = (context.ticks + context.transition) * 50.0F;
         int oldest = index;

         while (oldest - 1 >= 0 && now - (float)((Lrc.Line)lines.get(oldest)).time < 150.0F) {
            oldest--;
         }

         for (int i = oldest; i <= index; i++) {
            if (shows(lines, i)) {
               Lrc.Line line = (Lrc.Line)lines.get(i);
               TextFlashLayout.Block block = TextFlashLayout.build(line, nextTime(lines, i), style);
               float fade = i < index ? (now - (float)((Lrc.Line)lines.get(i + 1)).time) / 150.0F : -1.0F;
               frames.add(new TextFlashClip.Frame(this, block, now, fade, envelope, beatAudio, beatMillis, clock));
            }
         }
      }
   }

   protected Clip create() {
      return new TextFlashClip();
   }

   public static final class Frame {
      public final TextFlashClip clip;
      public final TextFlashLayout.Block block;
      public final float now;
      public final float fade;
      public final float alpha;
      public final Link beatAudio;
      public final float beatMillis;
      public final float clock;

      public Frame(TextFlashClip clip, TextFlashLayout.Block block, float now, float fade, float alpha, Link beatAudio, float beatMillis, float clock) {
         this.clip = clip;
         this.block = block;
         this.now = now;
         this.fade = fade;
         this.alpha = alpha;
         this.beatAudio = beatAudio;
         this.beatMillis = beatMillis;
         this.clock = clock;
      }
   }
}
