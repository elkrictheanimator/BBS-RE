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
import net.minecraftforge.registries.ForgeRegistries;
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
     * The mod registers blocks, entities and so on with {@code Registry.register} from static fields and
     * its initializer. Vanilla registries are open only while Forge fires its register events, so the
     * whole initializer runs inside the first one.
     */
    private static void onRegister(RegisterEvent event)
    {
        if (!initialized && event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS))
        {
            initialized = true;

            ForgeNetwork.channel();
            new BBSMod().onInitialize();
        }
    }

    private static void onAttributes(EntityAttributeCreationEvent event)
    {
        FabricDefaultAttributeRegistry.PENDING.forEach(event::put);
    }
}
