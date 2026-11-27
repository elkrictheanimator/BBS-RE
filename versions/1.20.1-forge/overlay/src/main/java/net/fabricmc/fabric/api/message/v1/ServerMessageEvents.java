package net.fabricmc.fabric.api.message.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.message.MessageType;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerMessageEvents
{
    public static final Event<ChatMessage> CHAT_MESSAGE = EventFactory.createArrayBacked(ChatMessage.class, (callbacks) -> (message, sender, params) ->
    {
        for (ChatMessage callback : callbacks)
        {
            callback.onChatMessage(message, sender, params);
        }
    });

    public interface ChatMessage
    {
        void onChatMessage(SignedMessage message, ServerPlayerEntity sender, MessageType.Parameters params);
    }
}
