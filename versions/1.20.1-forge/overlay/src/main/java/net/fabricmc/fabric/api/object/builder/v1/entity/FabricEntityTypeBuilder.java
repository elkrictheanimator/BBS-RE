package net.fabricmc.fabric.api.object.builder.v1.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;

public final class FabricEntityTypeBuilder<T extends Entity>
{
    private final EntityType.Builder<T> builder;

    private FabricEntityTypeBuilder(SpawnGroup group, EntityType.EntityFactory<T> factory)
    {
        this.builder = EntityType.Builder.create(factory, group);
    }

    public static <T extends Entity> FabricEntityTypeBuilder<T> create(SpawnGroup group, EntityType.EntityFactory<T> factory)
    {
        return new FabricEntityTypeBuilder<>(group, factory);
    }

    public FabricEntityTypeBuilder<T> dimensions(EntityDimensions dimensions)
    {
        this.builder.setDimensions(dimensions.width, dimensions.height);

        return this;
    }

    public FabricEntityTypeBuilder<T> trackRangeBlocks(int blocks)
    {
        this.builder.maxTrackingRange(Math.max(1, (blocks + 15) / 16));

        return this;
    }

    public FabricEntityTypeBuilder<T> trackRangeChunks(int chunks)
    {
        this.builder.maxTrackingRange(chunks);

        return this;
    }

    public FabricEntityTypeBuilder<T> trackedUpdateRate(int rate)
    {
        this.builder.trackingTickInterval(rate);

        return this;
    }

    public EntityType<T> build()
    {
        return this.builder.build("bbs");
    }
}
