package mchorse.bbs_mod.forge;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/** Client half of {@link ForgeNetwork}; kept apart so a dedicated server never loads it. */
public class ForgeClientNetwork
{
    public static void receive(Identifier id, byte[] data)
    {
        ClientPlayNetworking.PlayChannelHandler handler = ForgeNetwork.CLIENT_HANDLERS.get(id);
        MinecraftClient client = MinecraftClient.getInstance();

        if (handler != null)
        {
            handler.receive(client, client.getNetworkHandler(), new PacketByteBuf(Unpooled.wrappedBuffer(data)), (channel, buf) -> ForgeNetwork.sendToServer(channel, buf));
        }
    }
}
