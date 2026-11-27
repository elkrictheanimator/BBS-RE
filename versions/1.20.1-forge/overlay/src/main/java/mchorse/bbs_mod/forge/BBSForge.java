package mchorse.bbs_mod.forge;

import mchorse.bbs_mod.BBSMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge entry point. The mod itself is written against Fabric's API; this class drives its
 * {@code ModInitializer} / {@code ClientModInitializer} and turns Forge's events into Fabric's.
 */
@Mod("bbs")
public class BBSForge
{
    private static boolean initialized;

    public BBSForge()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(BBSForge::onRegister);
        bus.addListener(BBSForge::onAttributes);

        ForgeEventBridge.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ForgeClientBridge.init(bus));
    }

    /**
     * The mod registers blocks, entities and so on from static fields and its initializer. Those calls are
     * queued (see {@link ForgeRegistry}) and handed to Forge when the event of each registry fires.
     */
    private static void onRegister(RegisterEvent event)
    {
        /* Blocks, items and entity types take an intrusive holder in their constructors, which only works
         * while the registries are open, so the initializer waits for the first register event */
        if (!initialized)
        {
            initialized = true;

            ForgeNetwork.channel();
            new BBSMod().onInitialize();
        }

        ForgeRegistry.flush(event);
    }

    private static void onAttributes(EntityAttributeCreationEvent event)
    {
        FabricDefaultAttributeRegistry.PENDING.forEach((type, container) -> event.put(type, container.get()));
    }
}
