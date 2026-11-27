package bbsfx.text;

import bbsfx.lyrics.Lrc;
import bbsfx.lyrics.LyricMotion;
import bbsfx.lyrics.LyricSplit;
import bbsfx.lyrics.LyricWords;
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

public class Lyrics3DClip extends MusicTextClip {
   public static final String FRAMES = "bbsfx:lyrics3d";
   public static final long FADE_MS = 400L;
   public static final float FADE_LIFT = 0.16F;
   public static final long WORD_STEP_MS = 330L;
   public final ValueFloat distance = new ValueFloat("distance", 10.0F, 3.0F, 40.0F);
   public final ValueFloat spread = new ValueFloat("spread", 30.0F, 0.0F, 60.0F);
   public final ValueFloat drop = new ValueFloat("drop", 10.0F, 0.0F, 40.0F);
   public final ValueFloat positionX = new ValueFloat("positionX", 0.0F, -100.0F, 100.0F);
   public final ValueFloat positionY = new ValueFloat("positionY", 0.0F, -100.0F, 100.0F);
   public final ValueFloat positionZ = new ValueFloat("positionZ", 0.0F, -100.0F, 100.0F);
   public final ValueFloat yawOffset = new ValueFloat("yawOffset", 0.0F, -180.0F, 180.0F);
   public final ValueFloat pitchOffset = new ValueFloat("pitchOffset", 0.0F, -180.0F, 180.0F);
   public final ValueFloat roll = new ValueFloat("roll", 0.0F, -180.0F, 180.0F);
   public final ValueFloat scale = new ValueFloat("scale", 1.0F, 0.5F, 3.0F);
   public final ValueBoolean faceCamera = new ValueBoolean("faceCamera", true);
   public final ValueBoolean keepAway = new ValueBoolean("keepAway", true);
   public final ValueBoolean aboveGround = new ValueBoolean("aboveGround", false);
   public final ValueBoolean throughWalls = new ValueBoolean("throughWalls", true);
   public final ValueBoolean followCamera = new ValueBoolean("followCamera", false);
   public final ValueString actor = new ValueString("actor", "");
   public final ValueFloat avoidMargin = new ValueFloat("avoidMargin", 0.03F, 0.0F, 0.3F);
   public final ValueBoolean smoothFont = new ValueBoolean("smoothFont", false);
   public final ValueInt fontStyle = new ValueInt("fontStyle", 0, 0, 2);
   public final ValueBoolean uppercase = new ValueBoolean("uppercase", false);
   public final ValueBoolean bilingual = new ValueBoolean("bilingual", false);
   public final ValueInt intro = new ValueInt("intro", 0, 0, 7);
   public final ValueInt charFx = new ValueInt("charFx", 0, 0, 2);
   public final ValueInt align = new ValueInt("align", 0, 0, 2);
   public final ValueLink font = new ValueLink("font", null);
   public final ValueInt color = new ValueInt("color", -532009);
   public final ValueBoolean gradient = new ValueBoolean("gradient", false);
   public final ValueInt gradientFrom = new ValueInt("gradientFrom", -1);
   public final ValueInt gradientTo = new ValueInt("gradientTo", -5737473);
   public final ValueInt gradientMode = new ValueInt("gradientMode", 0, 0, 1);
   public final ValueBoolean textShadow = new ValueBoolean("textShadow", true);
   public final ValueInt maxWidth = new ValueInt("maxWidth", 200, 40, 1000);
   public final ValueBoolean glow = new ValueBoolean("glow", true);
   public final ValueInt glowColor = new ValueInt("glowColor", -1);
   public final ValueFloat glowStrength = new ValueFloat("glowStrength", 0.3F, 0.0F, 3.0F);
   public final ValueFloat glowRadius = new ValueFloat("glowRadius", 0.5F, 0.0F, 1.0F);
   public final ValueBoolean wiggle = new ValueBoolean("wiggle", true);
   public final ValueInt wiggleFps = new ValueInt("wiggleFps", 8, 4, 24);
   public final ValueFloat wiggleAmount = new ValueFloat("wiggleAmount", 0.8F, 0.0F, 2.0F);
   public final ValueBoolean wordPop = new ValueBoolean("wordPop", true);
   public final ValueFloat beat = new ValueFloat("beat", 0.0F, 0.0F, 2.0F);

   public Lyrics3DClip() {
      this.add(this.distance);
      this.add(this.spread);
      this.add(this.drop);
      this.add(this.positionX);
      this.add(this.positionY);
      this.add(this.positionZ);
      this.add(this.yawOffset);
      this.add(this.pitchOffset);
      this.add(this.roll);
      this.add(this.scale);
      this.add(this.faceCamera);
      this.add(this.keepAway);
      this.add(this.aboveGround);
      this.add(this.throughWalls);
      this.add(this.followCamera);
      this.add(this.actor);
      this.add(this.avoidMargin);
      this.add(this.smoothFont);
      this.add(this.fontStyle);
      this.add(this.uppercase);
      this.add(this.bilingual);
      this.add(this.intro);
      this.add(this.charFx);
      this.add(this.align);
      this.add(this.font);
      this.add(this.color);
      this.add(this.gradient);
      this.add(this.gradientFrom);
      this.add(this.gradientTo);
      this.add(this.gradientMode);
      this.add(this.textShadow);
      this.add(this.maxWidth);
      this.add(this.glow);
      this.add(this.glowColor);
      this.add(this.glowStrength);
      this.add(this.glowRadius);
      this.add(this.wiggle);
      this.add(this.wiggleFps);
      this.add(this.wiggleAmount);
      this.add(this.wordPop);
      this.add(this.beat);
   }

   public static List<Lyrics3DClip.Frame> getFrames(ClipContext context) {
      return context.clipData.get("bbsfx:lyrics3d", () -> new ArrayList<Lyrics3DClip.Frame>());
   }

   @Override
   protected void collect(ClipContext context, Position position, List<Lrc.Line> lines, float now) {
      float envelope = this.envelopeFactor(context);
      if (!(envelope <= 0.0F)) {
         int index = LyricSplit.indexAt(lines, now);
         float clock = (context.ticks + context.transition) * 50.0F;
         List<Lyrics3DClip.Frame> frames = getFrames(context);

         for (int i = index - 1; i >= 0; i--) {
            float gone = now - (float)((Lrc.Line)lines.get(i + 1)).time;
            if (gone >= 400.0F) {
               break;
            }

            if (shows(lines, i)) {
               float t = gone / 400.0F;
               frames.add(this.frame(context, position, lines, i, now, envelope * (1.0F - t), t * 0.16F, clock));
            }
         }

         if (shows(lines, index)) {
            frames.add(this.frame(context, position, lines, index, now, envelope, 0.0F, clock));
         }
      }
   }

   private Lyrics3DClip.Frame frame(ClipContext context, Position position, List<Lrc.Line> lines, int i, float now, float alpha, float lift, float clock) {
      Lrc.Line line = (Lrc.Line)lines.get(i);
      long next = nextTime(lines, i);
      Position camera = this.cameraAt(context, line.time, position);
      int seed = (Integer)this.seed.get();
      double distance = ((Float)this.distance.get()).floatValue();
      float yaw = camera.angle.yaw + LyricMotion.signed(i, line.time, 31, seed) * (Float)this.spread.get();
      float pitch = camera.angle.pitch + (Float)this.drop.get() * (0.3F + 0.7F * LyricMotion.unit(i, line.time, 32, seed));
      pitch = Math.max(-75.0F, Math.min(75.0F, pitch));
      if ((Boolean)this.aboveGround.get()) {
         pitch = Math.min(pitch, LyricMotion.lowestPitch(distance, 0.5));
      }

      double[] forward = LyricMotion.forward(yaw, pitch);
      return new Lyrics3DClip.Frame(
         this,
         line.text,
         line.translation,
         this.charTimes(line, next),
         now - (float)line.time,
         next - line.time,
         alpha,
         lift,
         camera.point.x + forward[0] * distance + ((Float)this.positionX.get()).floatValue(),
         camera.point.y + forward[1] * distance + ((Float)this.positionY.get()).floatValue(),
         camera.point.z + forward[2] * distance + ((Float)this.positionZ.get()).floatValue(),
         yaw,
         pitch,
         clock,
         line.text.hashCode() * 31 + seed,
         this.beatAudio(context),
         this.beatMillis(context)
      );
   }

   private long[] charTimes(Lrc.Line line, long next) {
      String text = line.text;
      if (!(Boolean)this.wordPop.get()) {
         long[] times = new long[text.length()];
         int chars = Math.max(1, LyricMotion.visibleChars(text));
         float typing = LyricMotion.typeMillis(chars, next - line.time, 50.0F, 0.6F);
         int seen = 0;

         for (int c = 0; c < text.length(); c++) {
            times[c] = (long)(typing * seen / chars);
            if (!Character.isWhitespace(text.charAt(c)) && !Character.isLowSurrogate(text.charAt(c))) {
               seen++;
            }
         }

         return times;
      } else {
         long[] times = LyricWords.charTimes(line, next, 330L);

         for (int i = 0; i < times.length; i++) {
            times[i] = Math.max(0L, times[i] - line.time);
         }

         return times;
      }
   }

   public Link smoothFontLink() {
      return this.smoothFont.get() ? (Link)this.font.get() : null;
   }

   protected Clip create() {
      return new Lyrics3DClip();
   }

   public static final class Frame {
      public final Lyrics3DClip clip;
      public final String text;
      public final String translation;
      public final long[] charTimes;
      public final float elapsed;
      public final long length;
      public final float alpha;
      public final float lift;
      public final double x;
      public final double y;
      public final double z;
      public final float yaw;
      public final float pitch;
      public final float clock;
      public final int seed;
      public final Link beatAudio;
      public final float beatMillis;

      public Frame(
         Lyrics3DClip clip,
         String text,
         String translation,
         long[] charTimes,
         float elapsed,
         long length,
         float alpha,
         float lift,
         double x,
         double y,
         double z,
         float yaw,
         float pitch,
         float clock,
         int seed,
         Link beatAudio,
         float beatMillis
      ) {
         this.clip = clip;
         this.text = text;
         this.translation = translation == null ? "" : translation;
         this.charTimes = charTimes;
         this.elapsed = elapsed;
         this.length = length;
         this.alpha = alpha;
         this.lift = lift;
         this.x = x;
         this.y = y;
         this.z = z;
         this.yaw = yaw;
         this.pitch = pitch;
         this.clock = clock;
         this.seed = seed;
         this.beatAudio = beatAudio;
         this.beatMillis = beatMillis;
      }
   }
}
