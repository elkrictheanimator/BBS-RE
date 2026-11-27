package net.fabricmc.fabric.api.client.networking.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class ClientPlayConnectionEvents
{
    public static final Event<Disconnect> DISCONNECT = EventFactory.createArrayBacked(Disconnect.class, (callbacks) -> (handler, client) ->
    {
        for (Disconnect callback : callbacks)
        {
            callback.onPlayDisconnect(handler, client);
        }
    });

    public interface Disconnect
    {
        void onPlayDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client);
    }
}
