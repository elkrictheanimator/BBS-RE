package net.fabricmc.fabric.api.networking.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;

public final class ServerPlayConnectionEvents
{
    public static final Event<Join> JOIN = EventFactory.createArrayBacked(Join.class, (callbacks) -> (handler, sender, server) ->
    {
        for (Join callback : callbacks)
        {
            callback.onPlayReady(handler, sender, server);
        }
    });

    public static final Event<Disconnect> DISCONNECT = EventFactory.createArrayBacked(Disconnect.class, (callbacks) -> (handler, server) ->
    {
        for (Disconnect callback : callbacks)
        {
            callback.onPlayDisconnect(handler, server);
        }
    });

    public interface Join
    {
        void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server);
    }

    public interface Disconnect
    {
        void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server);
    }
}
