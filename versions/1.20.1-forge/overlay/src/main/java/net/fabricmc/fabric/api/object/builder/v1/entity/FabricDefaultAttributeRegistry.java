package net.fabricmc.fabric.api.object.builder.v1.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricDefaultAttributeRegistry
{
    /** Collected here, handed to Forge's EntityAttributeCreationEvent. */
    public static final Map<EntityType<? extends LivingEntity>, DefaultAttributeContainer> PENDING = new LinkedHashMap<>();

    public static void register(EntityType<? extends LivingEntity> type, DefaultAttributeContainer.Builder builder)
    {
        PENDING.put(type, builder.build());
    }
}
