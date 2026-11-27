package net.fabricmc.fabric.api.resource;

import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;

public interface IdentifiableResourceReloadListener extends ResourceReloader
{
    Identifier getFabricId();
}
