package mchorse.bbs_mod.forge;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.List;

/** Forge game events (both sides) turned into the Fabric events the mod listens to. */
public class ForgeEventBridge
{
    private record Broken(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity entity)
    {}

    /* Fabric's "after break" runs once the block is gone; Forge's break event runs before. */
    private static final List<Broken> broken = new ArrayList<>();

    public static void init()
    {
        var bus = MinecraftForge.EVENT_BUS;

        bus.addListener((ServerStartedEvent e) -> ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(e.getServer()));
        bus.addListener((ServerStoppedEvent e) -> ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(e.getServer()));

        bus.addListener((TickEvent.ServerTickEvent e) ->
        {
            if (e.phase == TickEvent.Phase.START)
            {
                ServerTickEvents.START_SERVER_TICK.invoker().onStartTick(e.getServer());
            }
            else
            {
                if (!broken.isEmpty())
                {
                    List<Broken> list = new ArrayList<>(broken);

                    broken.clear();

                    for (Broken b : list)
                    {
                        PlayerBlockBreakEvents.AFTER.invoker().afterBlockBreak(b.world, b.player, b.pos, b.state, b.entity);
                    }
                }

                ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(e.getServer());
            }
        });

        bus.addListener((EntityJoinLevelEvent e) ->
        {
            if (e.getLevel() instanceof ServerWorld world)
            {
                ServerEntityEvents.ENTITY_LOAD.invoker().onLoad(e.getEntity(), world);
            }
        });

        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) ->
        {
            if (e.getEntity() instanceof ServerPlayerEntity player)
            {
                ServerPlayConnectionEvents.JOIN.invoker().onPlayReady(player.networkHandler, ForgeNetwork.senderFor(player), player.server);
            }
        });

        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) ->
        {
            if (e.getEntity() instanceof ServerPlayerEntity player)
            {
                ServerPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(player.networkHandler, player.server);
            }
        });

        bus.addListener((PlayerEvent.StartTracking e) ->
        {
            if (e.getEntity() instanceof ServerPlayerEntity player)
            {
                EntityTrackingEvents.START_TRACKING.invoker().onStartTracking(e.getTarget(), player);
            }
        });

        bus.addListener((RegisterCommandsEvent e) -> CommandRegistrationCallback.EVENT.invoker().register(e.getDispatcher(), e.getBuildContext(), e.getCommandSelection()));

        bus.addListener((ServerChatEvent e) -> ServerMessageEvents.CHAT_MESSAGE.invoker().onChatMessage(SignedMessage.ofUnsigned(e.getRawText()), e.getPlayer(), null));

        bus.addListener((BlockEvent.BreakEvent e) ->
        {
            if (!e.isCanceled() && e.getLevel() instanceof World world && !world.isClient())
            {
                broken.add(new Broken(world, e.getPlayer(), e.getPos(), e.getState(), world.getBlockEntity(e.getPos())));
            }
        });

        bus.addListener((PlayerInteractEvent.LeftClickBlock e) ->
        {
            ActionResult result = AttackBlockCallback.EVENT.invoker().interact(e.getEntity(), e.getLevel(), e.getHand(), e.getPos(), e.getFace());

            if (result != ActionResult.PASS)
            {
                e.setCanceled(true);
            }
        });

        bus.addListener((PlayerInteractEvent.RightClickBlock e) ->
        {
            ActionResult result = UseBlockCallback.EVENT.invoker().interact(e.getEntity(), e.getLevel(), e.getHand(), e.getHitVec());

            if (result != ActionResult.PASS)
            {
                e.setCanceled(true);
            }
        });
    }
}
