package bbsfx.client.forms.renderers;

import bbsfx.FilmTimeline;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.entities.IEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class FilmClock {
   private static final FilmTimeline TIMELINE = new FilmTimeline();

   private FilmClock() {
   }

   public static void capture(BaseFilmController controller, int updateTick) {
      TIMELINE.begin(controller);
      if (controller.film != null) {
         int filmTick = controller.getTick();

         for (Replay replay : controller.film.replays.getList()) {
            IEntity entity = (IEntity)controller.entities.get(replay.getId());
            if (entity != null) {
               TIMELINE.put(controller, entity, replay.getTick(filmTick));
            }
         }
      }
   }

   public static void clear(BaseFilmController controller) {
      TIMELINE.clear(controller);
   }

   public static Float time(IEntity entity, float transition) {
      return TIMELINE.time(entity, transition);
   }
}
