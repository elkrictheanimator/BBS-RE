package net.fabricmc.fabric.api.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;

public final class ServerLifecycleEvents
{
    public static final Event<ServerStarted> SERVER_STARTED = EventFactory.createArrayBacked(ServerStarted.class, (callbacks) -> (server) ->
    {
        for (ServerStarted callback : callbacks)
        {
            callback.onServerStarted(server);
        }
    });

    public static final Event<ServerStopped> SERVER_STOPPED = EventFactory.createArrayBacked(ServerStopped.class, (callbacks) -> (server) ->
    {
        for (ServerStopped callback : callbacks)
        {
            callback.onServerStopped(server);
        }
    });

    public interface ServerStarted
    {
        void onServerStarted(MinecraftServer server);
    }

    public interface ServerStopped
    {
        void onServerStopped(MinecraftServer server);
    }
}
