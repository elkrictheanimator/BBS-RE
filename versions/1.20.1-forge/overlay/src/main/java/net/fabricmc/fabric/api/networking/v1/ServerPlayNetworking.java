package net.fabricmc.fabric.api.networking.v1;

import mchorse.bbs_mod.forge.ForgeNetwork;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class ServerPlayNetworking
{
    public static void registerGlobalReceiver(Identifier id, PlayChannelHandler handler)
    {
        ForgeNetwork.SERVER_HANDLERS.put(id, handler);
    }

    public static void send(ServerPlayerEntity player, Identifier id, PacketByteBuf buf)
    {
        ForgeNetwork.sendToPlayer(player, id, buf);
    }

    @FunctionalInterface
    public interface PlayChannelHandler
    {
        void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender);
    }
}
