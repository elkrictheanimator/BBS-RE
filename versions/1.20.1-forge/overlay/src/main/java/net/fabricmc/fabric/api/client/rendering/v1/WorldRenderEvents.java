package net.fabricmc.fabric.api.client.rendering.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class WorldRenderEvents
{
    public static final Event<Stage> BEFORE_ENTITIES = create();
    public static final Event<Stage> AFTER_ENTITIES = create();
    public static final Event<Stage> LAST = create();

    private static Event<Stage> create()
    {
        return EventFactory.createArrayBacked(Stage.class, (callbacks) -> (context) ->
        {
            for (Stage callback : callbacks)
            {
                callback.onStage(context);
            }
        });
    }

    @FunctionalInterface
    public interface Stage
    {
        void onStage(WorldRenderContext context);
    }
}
