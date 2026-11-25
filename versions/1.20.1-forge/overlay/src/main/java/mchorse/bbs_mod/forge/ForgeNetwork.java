package mchorse.bbs_mod.forge;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Fabric's identifier-based packets, carried over one Forge channel: [identifier][raw bytes]. */
public class ForgeNetwork
{
    public static final Map<Identifier, ServerPlayNetworking.PlayChannelHandler> SERVER_HANDLERS = new HashMap<>();
    public static final Map<Identifier, ClientPlayNetworking.PlayChannelHandler> CLIENT_HANDLERS = new HashMap<>();

    private static final String VERSION = "1";
    private static SimpleChannel channel;

    public static synchronized SimpleChannel channel()
    {
        if (channel == null)
        {
            channel = NetworkRegistry.newSimpleChannel(new Identifier("bbs", "main"), () -> VERSION, (v) -> true, (v) -> true);
            channel.registerMessage(0, Message.class, Message::encode, Message::decode, ForgeNetwork::handle);
        }

        return channel;
    }

    public static void sendToPlayer(ServerPlayerEntity player, Identifier id, PacketByteBuf buf)
    {
        channel().send(PacketDistributor.PLAYER.with(() -> player), new Message(id, copy(buf)));
    }

    public static void sendToServer(Identifier id, PacketByteBuf buf)
    {
        channel().sendToServer(new Message(id, copy(buf)));
    }

    public static PacketSender senderFor(ServerPlayerEntity player)
    {
        return (id, buf) -> sendToPlayer(player, id, buf);
    }

    private static byte[] copy(ByteBuf buf)
    {
        byte[] bytes = new byte[buf.readableBytes()];

        buf.getBytes(buf.readerIndex(), bytes);

        return bytes;
    }

    private static void handle(Message message, Supplier<NetworkEvent.Context> supplier)
    {
        NetworkEvent.Context context = supplier.get();

        if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER)
        {
            ServerPlayerEntity player = context.getSender();

            context.enqueueWork(() ->
            {
                ServerPlayNetworking.PlayChannelHandler handler = SERVER_HANDLERS.get(message.id);

                if (handler != null && player != null)
                {
                    handler.receive(player.server, player, player.networkHandler, new PacketByteBuf(Unpooled.wrappedBuffer(message.data)), senderFor(player));
                }
            });
        }
        else
        {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> context.enqueueWork(() -> ForgeClientNetwork.receive(message.id, message.data)));
        }

        context.setPacketHandled(true);
    }

    public static class Message
    {
        public final Identifier id;
        public final byte[] data;

        public Message(Identifier id, byte[] data)
        {
            this.id = id;
            this.data = data;
        }

        public static void encode(Message message, net.minecraft.network.PacketByteBuf buf)
        {
            buf.writeIdentifier(message.id);
            buf.writeByteArray(message.data);
        }

        public static Message decode(net.minecraft.network.PacketByteBuf buf)
        {
            Identifier id = buf.readIdentifier();

            return new Message(id, buf.readByteArray());
        }
    }
}
