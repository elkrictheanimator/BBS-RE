package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;

public final class ServerEntityEvents
{
    public static final Event<Load> ENTITY_LOAD = EventFactory.createArrayBacked(Load.class, (callbacks) -> (entity, world) ->
    {
        for (Load callback : callbacks)
        {
            callback.onLoad(entity, world);
        }
    });

    public interface Load
    {
        void onLoad(Entity entity, ServerWorld world);
    }
}
