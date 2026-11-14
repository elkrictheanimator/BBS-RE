package bbsfx;

import java.util.IdentityHashMap;
import java.util.Map;

public final class FilmTimeline {
   private final Map<Object, FilmTimeline.Entry> entries = new IdentityHashMap();

   public void begin(Object controller) {
      this.clear(controller);
   }

   public void put(Object controller, Object entity, int tick) {
      if (controller != null && entity != null) {
         this.entries.put(entity, new FilmTimeline.Entry(controller, tick));
      }
   }

   public Float time(Object entity, float transition) {
      FilmTimeline.Entry entry = (FilmTimeline.Entry)this.entries.get(entity);
      return entry == null ? null : entry.tick + transition;
   }

   public void clear(Object controller) {
      this.entries.values().removeIf(entry -> entry.controller == controller);
   }

   private record Entry(Object controller, int tick) {
   }
}
