package net.fabricmc.fabric.api.object.builder.v1.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class FabricDefaultAttributeRegistry
{
    /** Collected here, handed to Forge's EntityAttributeCreationEvent. */
    public static final Map<EntityType<? extends LivingEntity>, Supplier<DefaultAttributeContainer>> PENDING = new LinkedHashMap<>();

    public static void register(EntityType<? extends LivingEntity> type, DefaultAttributeContainer.Builder builder)
    {
        PENDING.put(type, builder::build);
    }

    /** Forge's own attributes are not registered yet when the mod initialises, so the builder is made on demand. */
    public static void registerLazy(EntityType<? extends LivingEntity> type, Supplier<DefaultAttributeContainer.Builder> builder)
    {
        PENDING.put(type, () -> builder.get().build());
    }
}
