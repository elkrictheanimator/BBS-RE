package bbsfx.lyrics;

import java.util.Arrays;

public final class BeatMap {
   public static final BeatMap EMPTY = new BeatMap(new float[0], new float[0]);
   public static final float FRAME_MS = 10.0F;
   private static final int WINDOW = 50;
   private static final float THRESHOLD = 1.6F;
   private static final float MIN_GAP_MS = 150.0F;
   private static final float SILENCE = 1.0E-5F;
   private final float[] times;
   private final float[] strengths;

   private BeatMap(float[] times, float[] strengths) {
      this.times = times;
      this.strengths = strengths;
   }

   public int size() {
      return this.times.length;
   }

   public float time(int index) {
      return this.times[index];
   }

   public float strength(int index) {
      return this.strengths[index];
   }

   public float pulse(float ms, float decayMs) {
      int index = Arrays.binarySearch(this.times, ms);
      if (index < 0) {
         index = -index - 2;
      }

      float best = 0.0F;

      for (int i = index; i >= 0; i--) {
         float age = ms - this.times[i];
         if (age > decayMs * 6.0F) {
            break;
         }

         best = Math.max(best, this.strengths[i] * (float)Math.exp(-age / decayMs));
      }

      return best;
   }

   public static BeatMap fromEnergy(float[] energy) {
      double total = 0.0;

      for (float e : energy) {
         total += e;
      }

      float floor = Math.max(1.0E-5F, energy.length == 0 ? 0.0F : (float)(total / energy.length) * 0.15F);
      float[] times = new float[16];
      float[] strengths = new float[16];
      int count = 0;
      float last = Float.NEGATIVE_INFINITY;
      double sum = 0.0;

      for (int n = 0; n < energy.length; n++) {
         int window = Math.min(n, 50);
         float average = window == 0 ? 0.0F : (float)(sum / window);
         float e = energy[n];
         boolean peak = (n == 0 || e >= energy[n - 1]) && (n + 1 >= energy.length || e > energy[n + 1]);
         float time = n * 10.0F;
         if (window >= 5 && peak && e > floor && e > average * 1.6F && time - last >= 150.0F) {
            float ratio = average <= 0.0F ? 4.0F : e / average;
            if (count == times.length) {
               times = Arrays.copyOf(times, count * 2);
               strengths = Arrays.copyOf(strengths, count * 2);
            }

            times[count] = time;
            strengths[count] = Math.max(0.25F, Math.min(1.0F, (ratio - 1.0F) / 3.0F));
            count++;
            last = time;
         }

         sum += e;
         if (n >= 50) {
            sum -= energy[n - 50];
         }
      }

      return new BeatMap(Arrays.copyOf(times, count), Arrays.copyOf(strengths, count));
   }

   public static final class Builder {
      private static final double CUTOFF_HZ = 150.0;
      private final float alpha;
      private final int hop;
      private float low1;
      private float low2;
      private double sum;
      private int count;
      private float[] frames = new float[1024];
      private int size;

      public Builder(int sampleRate) {
         int rate = Math.max(1, sampleRate);
         this.alpha = (float)(1.0 - Math.exp(-942.4777960769379 / rate));
         this.hop = Math.max(1, Math.round(rate * 10.0F / 1000.0F));
      }

      public void add(float sample) {
         this.low1 = this.low1 + this.alpha * (sample - this.low1);
         this.low2 = this.low2 + this.alpha * (this.low1 - this.low2);
         this.sum = this.sum + this.low2 * this.low2;
         if (++this.count >= this.hop) {
            if (this.size == this.frames.length) {
               this.frames = Arrays.copyOf(this.frames, this.size * 2);
            }

            this.frames[this.size++] = (float)(this.sum / this.count);
            this.sum = 0.0;
            this.count = 0;
         }
      }

      public BeatMap build() {
         return BeatMap.fromEnergy(Arrays.copyOf(this.frames, this.size));
      }
   }
}
