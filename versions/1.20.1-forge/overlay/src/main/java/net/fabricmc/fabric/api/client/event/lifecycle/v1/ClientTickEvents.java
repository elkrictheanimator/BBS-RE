package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public final class ClientTickEvents
{
    public static final Event<StartTick> START_CLIENT_TICK = EventFactory.createArrayBacked(StartTick.class, (callbacks) -> (client) ->
    {
        for (StartTick callback : callbacks)
        {
            callback.onStartTick(client);
        }
    });

    public static final Event<EndTick> END_CLIENT_TICK = EventFactory.createArrayBacked(EndTick.class, (callbacks) -> (client) ->
    {
        for (EndTick callback : callbacks)
        {
            callback.onEndTick(client);
        }
    });

    public static final Event<EndWorldTick> END_WORLD_TICK = EventFactory.createArrayBacked(EndWorldTick.class, (callbacks) -> (world) ->
    {
        for (EndWorldTick callback : callbacks)
        {
            callback.onEndTick(world);
        }
    });

    public interface StartTick
    {
        void onStartTick(MinecraftClient client);
    }

    public interface EndTick
    {
        void onEndTick(MinecraftClient client);
    }

    public interface EndWorldTick
    {
        void onEndTick(ClientWorld world);
    }
}
