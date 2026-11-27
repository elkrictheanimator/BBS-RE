package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.utils.clips.Clip;

import java.util.ArrayList;
import java.util.List;

/**
 * Bound shots: a camera clip can name the replays (actors) that belong to it and the replay tick its
 * start corresponds to. Moving the clip on the timeline then keeps the actor's performance in the
 * shot, and an actor is hidden while none of its shots is on.
 */
public class ShotRemapper
{
    public static List<String> actorIds(String list)
    {
        List<String> ids = new ArrayList<>();

        if (list != null)
        {
            for (String id : list.split(","))
            {
                if (!id.trim().isEmpty())
                {
                    ids.add(id.trim());
                }
            }
        }

        return ids;
    }

    public static boolean isBound(CameraClip clip, String replayId)
    {
        return actorIds(clip.boundActors.get()).contains(replayId);
    }

    public static Film findFilm(BaseValue value)
    {
        while (value != null)
        {
            if (value instanceof Film film)
            {
                return film;
            }

            value = value.getParent();
        }

        return null;
    }

    /** Whether this camera clip names this replay, without splitting or allocating anything. */
    private static boolean bound(Clip clip, String replayId)
    {
        if (!clip.enabled.get() || !(clip instanceof CameraClip shot))
        {
            return false;
        }

        String list = shot.boundActors.get();

        if (list.isEmpty())
        {
            return false;
        }

        return list.equals(replayId) || ("," + list + ",").contains("," + replayId + ",");
    }

    private static int source(CameraClip shot)
    {
        int source = shot.boundSource.get();

        return source < 0 ? shot.tick.get() : source;
    }

    /** The replay tick a film tick plays: inside a bound shot it follows the shot, around it it holds. */
    public static int remap(Replay replay, int tick)
    {
        Film film = findFilm(replay);

        if (film == null)
        {
            return tick;
        }

        String id = replay.getId();
        CameraClip inside = null;
        CameraClip before = null;
        CameraClip after = null;
        boolean any = false;

        for (Clip clip : film.camera.get())
        {
            if (!bound(clip, id))
            {
                continue;
            }

            CameraClip shot = (CameraClip) clip;
            int start = shot.tick.get();
            int end = start + shot.duration.get();

            any = true;

            if (tick >= start && tick < end)
            {
                if (inside == null || shot.layer.get() > inside.layer.get())
                {
                    inside = shot;
                }
            }
            else if (end <= tick)
            {
                if (before == null || end > before.tick.get() + before.duration.get())
                {
                    before = shot;
                }
            }
            else if (after == null || start < after.tick.get())
            {
                after = shot;
            }
        }

        if (!any)
        {
            return tick;
        }

        if (inside != null)
        {
            return source(inside) + (tick - inside.tick.get());
        }

        if (before != null)
        {
            return source(before) + before.duration.get();
        }

        return after == null ? tick : source(after);
    }

    /** Whether the actor should not be seen: it has shots, and none of them is on at this tick. */
    public static boolean isHidden(Replay replay, int tick)
    {
        Film film = findFilm(replay);

        if (film == null)
        {
            return false;
        }

        String id = replay.getId();
        boolean any = false;

        for (Clip clip : film.camera.get())
        {
            if (!bound(clip, id))
            {
                continue;
            }

            int start = clip.tick.get();

            any = true;

            if (tick >= start && tick < start + clip.duration.get())
            {
                return false;
            }
        }

        return any;
    }
}
