package bbsfx.lyrics;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.clips.misc.AudioClip;
import mchorse.bbs_mod.camera.clips.misc.CurveClip;
import mchorse.bbs_mod.camera.clips.misc.ImageClip;
import mchorse.bbs_mod.camera.clips.misc.SubtitleClip;
import mchorse.bbs_mod.camera.clips.misc.VideoClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;

public abstract class MusicTextClip extends CameraClip {
   public final ValueLink lrc = new ValueLink("lrc", null);
   public final ValueLink audio = new ValueLink("audio", null);
   public final ValueLink translation = new ValueLink("translation", null);
   public final ValueBoolean swapTranslation = new ValueBoolean("swapTranslation", false);
   public final ValueFloat offset = new ValueFloat("offset", 0.0F, -60000.0F, 60000.0F);
   public final ValueBoolean splitLines = new ValueBoolean("splitLines", true);
   public final ValueInt seed = new ValueInt("seed", 0, 0, 9999);
   public final ValueBoolean phraseMode = new ValueBoolean("phraseMode", false);
   public final ValueString phrases = new ValueString("phrases", "");
   public final ValueInt phraseTemplate = new ValueInt("phraseTemplate", 0, 0, 2);
   public final ValueFloat phraseDuration = new ValueFloat("phraseDuration", 2.0F, 0.2F, 10.0F);
   public final ValueBoolean phraseLoop = new ValueBoolean("phraseLoop", true);
   public final ValueBoolean segmentMode = new ValueBoolean("segmentMode", false);
   public final ValueString segmentText = new ValueString("segmentText", "");
   public final ValueString segmentTranslation = new ValueString("segmentTranslation", "");
   public static final Predicate<Clip> CAMERA_ONLY = clip -> !(clip instanceof AudioClip)
      && !(clip instanceof SubtitleClip)
      && !(clip instanceof ImageClip)
      && !(clip instanceof CurveClip)
      && !(clip instanceof VideoClip)
      && !(clip instanceof MusicTextClip);

   public MusicTextClip() {
      this.add(this.lrc);
      this.add(this.audio);
      this.add(this.translation);
      this.add(this.swapTranslation);
      this.add(this.offset);
      this.add(this.splitLines);
      this.add(this.seed);
      this.add(this.phraseMode);
      this.add(this.phrases);
      this.add(this.phraseTemplate);
      this.add(this.phraseDuration);
      this.add(this.phraseLoop);
      this.add(this.segmentMode);
      this.add(this.segmentText);
      this.add(this.segmentTranslation);
   }

   public AudioClip sourceAudio(ClipContext context) {
      return LyricClock.sourceAudio(context, (Link)this.audio.get());
   }

   public float songMillis(ClipContext context) {
      return LyricClock.millis(context, this.sourceAudio(context), (Float)this.offset.get());
   }

   public Link lyricLink(ClipContext context) {
      return LyricClock.lyricLink(context, (Link)this.lrc.get(), (Link)this.audio.get());
   }

   public List<Lrc.Line> lines(ClipContext context) {
      Link link = this.lyricLink(context);
      Lrc data = this.effectiveLyrics(link, Lrc.get(link));
      return data == null ? Collections.emptyList() : LyricSplit.view(data, (Boolean)this.splitLines.get());
   }

   public Lrc effectiveLyrics(Link link, Lrc data) {
      if (data == null) {
         return null;
      }

      Link file = (Link)this.translation.get();
      if (file != null && file.equals(link)) {
         file = null;
      }

      if (file == null) {
         file = Lrc.findTranslation(link);
      }

      Lrc sidecar = file == null ? null : Lrc.get(file);
      if (sidecar != null) {
         data = data.translatedBy(sidecar, file.toString());
      }

      return this.swapTranslation.get() ? data.swapped() : data;
   }

   public Link beatAudio(ClipContext context) {
      AudioClip clip = this.sourceAudio(context);
      return clip == null ? null : (Link)clip.audio.get();
   }

   public float beatMillis(ClipContext context) {
      AudioClip clip = this.sourceAudio(context);
      return clip == null ? -1.0F : LyricClock.songTicks(clip, context.ticks, context.transition) * 50.0F;
   }

   public float envelopeFactor(ClipContext context) {
      return this.envelope.factorEnabled((Integer)this.duration.get(), context.relativeTick + context.transition);
   }

   protected Position cameraAt(ClipContext context, double songMillis, Position current) {
      Position camera = new Position();
      double ticks = LyricClock.filmTicks(this, this.sourceAudio(context), (Float)this.offset.get(), songMillis);
      int whole = (int)Math.floor(ticks);
      camera.copy(current);
      if (!context.applyUnderneath(whole, (float)(ticks - whole), camera, CAMERA_ONLY)) {
         camera.copy(current);
      }

      return camera;
   }

   public List<Lrc.Line> segmentLines(ClipContext context) {
      if (!(Boolean)this.segmentMode.get()) {
         return Collections.emptyList();
      }

      String text = (String)this.segmentText.get();
      AudioClip audio = this.sourceAudio(context);
      long start = Math.round((audio == null ? 0.0F : LyricClock.songTicks(audio, (Integer)this.tick.get(), 0.0F) * 50.0F) + (Float)this.offset.get());
      long end = start + Math.max(0, (Integer)this.duration.get()) * 50L;
      return List.of(new Lrc.Line(start, text, null, (String)this.segmentTranslation.get()), new Lrc.Line(end, "", null));
   }

   protected void applyClip(ClipContext context, Position position) {
      float now = this.songMillis(context);
      List<Lrc.Line> lines = this.segmentLines(context);
      if (lines.isEmpty()) {
         lines = this.lines(context);
         if ((Boolean)this.phraseMode.get()) {
            lines = LyricPhrases.view((String)this.phrases.get(), lines, now, (Float)this.phraseDuration.get() * 1000.0F, (Boolean)this.phraseLoop.get());
         }
      }

      if (!lines.isEmpty()) {
         this.collect(context, position, lines, now);
      }
   }

   protected abstract void collect(ClipContext var1, Position var2, List<Lrc.Line> var3, float var4);

   protected static boolean shows(List<Lrc.Line> lines, int i) {
      return i >= 0 && i < lines.size() && !((Lrc.Line)lines.get(i)).text.isBlank();
   }

   protected static long nextTime(List<Lrc.Line> lines, int i) {
      return i + 1 < lines.size() ? ((Lrc.Line)lines.get(i + 1)).time : ((Lrc.Line)lines.get(i)).time + 5000L;
   }
}
