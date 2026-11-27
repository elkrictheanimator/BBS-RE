package mchorse.bbs_mod.forge;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.InvalidateRenderStateCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.impl.client.rendering.BlockEntityRendererRegistryImpl;
import net.fabricmc.fabric.impl.client.rendering.WorldRenderContextImpl;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.camera.controller.PlayCameraController;
import mchorse.bbs_mod.client.BBSRendering;
import mchorse.bbs_mod.client.HandHudFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Client-only Forge events turned into Fabric's; only ever loaded on the physical client. */
public class ForgeClientBridge
{
    private static boolean initialized;
    private static boolean started;
    private static final WorldRenderContextImpl CONTEXT = new WorldRenderContextImpl();

    private static synchronized void ensureInitialized()
    {
        if (!initialized)
        {
            initialized = true;

            new BBSModClient().onInitializeClient();
        }
    }

    private static void ensureStarted(MinecraftClient mc)
    {
        if (!started && mc != null && mc.getWindow() != null)
        {
            started = true;

            ClientLifecycleEvents.CLIENT_STARTED.invoker().onClientStarted(mc);
        }
    }

    public static void init(IEventBus modBus)
    {
        /* Forge fires RegisterClientReloadListenersEvent and EntityRenderersEvent.RegisterRenderers
         * BEFORE RegisterKeyMappingsEvent in ForgeHooksClient.initClientHooks, so ensureInitialized()
         * runs on whichever arrives first before draining the pending registration lists. */
        modBus.addListener((RegisterKeyMappingsEvent e) ->
        {
            ensureInitialized();

            KeyBindingHelper.PENDING.forEach(e::register);
        });

        modBus.addListener((EntityRenderersEvent.RegisterRenderers e) ->
        {
            ensureInitialized();

            for (EntityRendererRegistry.Entry entry : EntityRendererRegistry.PENDING)
            {
                register(e, entry);
            }

            for (BlockEntityRendererRegistryImpl.Entry entry : BlockEntityRendererRegistryImpl.PENDING)
            {
                registerBlockEntity(e, entry);
            }
        });

        modBus.addListener((RegisterClientReloadListenersEvent e) ->
        {
            ensureInitialized();

            ResourceManagerHelper.CLIENT.forEach(e::registerReloadListener);

            e.registerReloadListener((SynchronousResourceReloader) (manager) -> InvalidateRenderStateCallback.EVENT.invoker().onInvalidate());
        });

        modBus.addListener((FMLClientSetupEvent e) ->
        {
            e.enqueueWork(() ->
            {
                ensureInitialized();
                ensureStarted(MinecraftClient.getInstance());
            });
        });

        var bus = MinecraftForge.EVENT_BUS;

        bus.addListener((TickEvent.ClientTickEvent e) ->
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (e.phase == TickEvent.Phase.START)
            {
                ensureStarted(mc);

                ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(mc);
            }
            else
            {
                ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(mc);
            }
        });

        bus.addListener((TickEvent.LevelTickEvent e) ->
        {
            if (e.phase == TickEvent.Phase.END && e.level instanceof ClientWorld world)
            {
                ClientTickEvents.END_WORLD_TICK.invoker().onEndTick(world);
            }
        });

        bus.addListener((ClientPlayerNetworkEvent.LoggingOut e) ->
        {
            MinecraftClient mc = MinecraftClient.getInstance();

            ClientPlayConnectionEvents.DISCONNECT.invoker().onPlayDisconnect(mc.getNetworkHandler(), mc);
        });

        bus.addListener((GameShuttingDownEvent e) -> ClientLifecycleEvents.CLIENT_STOPPING.invoker().onClientStopping(MinecraftClient.getInstance()));

        /* Forge replaces vanilla InGameHud with ForgeGui, whose render() does not call super.render(),
         * so InGameHudMixin never fires on Forge. Mirror both its HEAD (HUD suppression during film
         * playback) and TAIL (capturing the preview/export texture and restoring mc.framebuffer before
         * the screen draws) through Forge's GUI and screen events. */
        bus.addListener((RenderGuiEvent.Pre e) ->
        {
            ICameraController current = BBSModClient.getCameraController().getCurrent();

            if (current instanceof PlayCameraController && !HandHudFlags.hud())
            {
                BBSRendering.onRenderBeforeScreen();
                e.setCanceled(true);
            }
        });

        bus.addListener((RenderGuiEvent.Post e) ->
        {
            HudRenderCallback.EVENT.invoker().onHudRender(e.getGuiGraphics(), e.getPartialTick());
            BBSRendering.onRenderBeforeScreen();
        });

        bus.addListener((ScreenEvent.Render.Pre e) ->
        {
            if (BBSRendering.isToggleFramebuffer())
            {
                BBSRendering.onRenderBeforeScreen();
            }
        });

        bus.addListener((ItemTooltipEvent e) -> ItemTooltipCallback.EVENT.invoker().getTooltip(e.getItemStack(), e.getFlags(), e.getToolTip()));

        bus.addListener((RenderLevelStageEvent e) ->
        {
            WorldRenderEvents.Stage target = null;
            RenderLevelStageEvent.Stage stage = e.getStage();

            /* Entities draw right after the cutout layers */
            if (stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
            {
                target = WorldRenderEvents.BEFORE_ENTITIES.invoker();
            }
            else if (stage == RenderLevelStageEvent.Stage.AFTER_ENTITIES)
            {
                target = WorldRenderEvents.AFTER_ENTITIES.invoker();
            }
            else if (stage == RenderLevelStageEvent.Stage.AFTER_LEVEL)
            {
                target = WorldRenderEvents.LAST.invoker();
            }

            if (target != null)
            {
                MinecraftClient mc = MinecraftClient.getInstance();

                CONTEXT.prepare(
                    mc.worldRenderer, e.getPoseStack(), e.getPartialTick(), e.getRenderTick(), false,
                    e.getCamera(), mc.gameRenderer, mc.gameRenderer.getLightmapTextureManager(),
                    e.getProjectionMatrix(), mc.getBufferBuilders().getEntityVertexConsumers(), null, false, mc.world
                );
                CONTEXT.setFrustum(e.getFrustum());

                target.onStage(CONTEXT);
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void register(EntityRenderersEvent.RegisterRenderers e, EntityRendererRegistry.Entry entry)
    {
        e.registerEntityRenderer((net.minecraft.entity.EntityType) entry.type(), (net.minecraft.client.render.entity.EntityRendererFactory) entry.factory());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerBlockEntity(EntityRenderersEvent.RegisterRenderers e, BlockEntityRendererRegistryImpl.Entry entry)
    {
        e.registerBlockEntityRenderer((net.minecraft.block.entity.BlockEntityType) entry.type(), (net.minecraft.client.render.block.entity.BlockEntityRendererFactory) entry.factory());
    }
}
