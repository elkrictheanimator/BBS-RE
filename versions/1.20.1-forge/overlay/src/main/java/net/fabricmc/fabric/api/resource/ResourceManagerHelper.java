package net.fabricmc.fabric.api.resource;

import net.minecraft.resource.ResourceReloader;
import net.minecraft.resource.ResourceType;

import java.util.ArrayList;
import java.util.List;

public final class ResourceManagerHelper
{
    /** Client reload listeners, handed to Forge's RegisterClientReloadListenersEvent. */
    public static final List<ResourceReloader> CLIENT = new ArrayList<>();

    private static final ResourceManagerHelper HELPER = new ResourceManagerHelper();

    public static ResourceManagerHelper get(ResourceType type)
    {
        return HELPER;
    }

    public void registerReloadListener(IdentifiableResourceReloadListener listener)
    {
        CLIENT.add(listener);
    }
}
