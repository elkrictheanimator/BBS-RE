package net.fabricmc.fabric.api.networking.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

public final class EntityTrackingEvents
{
    public static final Event<StartTracking> START_TRACKING = EventFactory.createArrayBacked(StartTracking.class, (callbacks) -> (entity, player) ->
    {
        for (StartTracking callback : callbacks)
        {
            callback.onStartTracking(entity, player);
        }
    });

    public interface StartTracking
    {
        void onStartTracking(Entity trackedEntity, ServerPlayerEntity player);
    }
}
