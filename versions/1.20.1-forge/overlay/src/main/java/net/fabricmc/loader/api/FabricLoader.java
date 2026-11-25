package net.fabricmc.loader.api;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** The slice of Fabric Loader's API that BBS touches, answered by Forge. */
public class FabricLoader
{
    private static final FabricLoader INSTANCE = new FabricLoader();

    public static FabricLoader getInstance()
    {
        return INSTANCE;
    }

    public boolean isModLoaded(String id)
    {
        return ModList.get() != null && ModList.get().isLoaded(id);
    }

    public Path getGameDir()
    {
        return FMLPaths.GAMEDIR.get();
    }

    public Path getConfigDir()
    {
        return FMLPaths.CONFIGDIR.get();
    }

    public boolean isDevelopmentEnvironment()
    {
        return !FMLLoader.isProduction();
    }

    public Optional<ModContainer> getModContainer(String id)
    {
        return ModList.get().getModContainerById(id).map((c) -> new ModContainer(c.getModInfo().getModId(), c.getModInfo().getVersion().toString()));
    }

    public Collection<ModContainer> getAllMods()
    {
        List<ModContainer> list = new ArrayList<>();

        ModList.get().getMods().forEach((info) -> list.add(new ModContainer(info.getModId(), info.getVersion().toString())));

        return list;
    }

    /** Addons are a Fabric entrypoint feature; none are discovered on Forge. */
    public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type)
    {
        return Collections.emptyList();
    }
}
