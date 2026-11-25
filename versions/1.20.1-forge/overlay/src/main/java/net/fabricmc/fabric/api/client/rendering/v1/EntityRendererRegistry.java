package net.fabricmc.fabric.api.client.rendering.v1;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public final class EntityRendererRegistry
{
    public record Entry(EntityType<? extends Entity> type, EntityRendererFactory<?> factory)
    {}

    /** Collected here, handed to Forge's RegisterRenderers event. */
    public static final List<Entry> PENDING = new ArrayList<>();

    public static <T extends Entity> void register(EntityType<? extends T> type, EntityRendererFactory<T> factory)
    {
        PENDING.add(new Entry(type, factory));
    }
}
