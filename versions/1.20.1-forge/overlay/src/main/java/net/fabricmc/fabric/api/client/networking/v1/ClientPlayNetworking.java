package net.fabricmc.fabric.api.client.networking.v1;

import mchorse.bbs_mod.forge.ForgeNetwork;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class ClientPlayNetworking
{
    public static void registerGlobalReceiver(Identifier id, PlayChannelHandler handler)
    {
        ForgeNetwork.CLIENT_HANDLERS.put(id, handler);
    }

    public static void send(Identifier id, PacketByteBuf buf)
    {
        ForgeNetwork.sendToServer(id, buf);
    }

    @FunctionalInterface
    public interface PlayChannelHandler
    {
        void receive(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender);
    }
}
