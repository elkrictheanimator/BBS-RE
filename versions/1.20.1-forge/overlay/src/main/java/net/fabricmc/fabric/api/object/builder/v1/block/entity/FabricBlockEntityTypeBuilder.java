package net.fabricmc.fabric.api.object.builder.v1.block.entity;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;

public final class FabricBlockEntityTypeBuilder<T extends BlockEntity>
{
    private final BlockEntityType.BlockEntityFactory<? extends T> factory;
    private final Block[] blocks;

    private FabricBlockEntityTypeBuilder(BlockEntityType.BlockEntityFactory<? extends T> factory, Block[] blocks)
    {
        this.factory = factory;
        this.blocks = blocks;
    }

    public static <T extends BlockEntity> FabricBlockEntityTypeBuilder<T> create(BlockEntityType.BlockEntityFactory<? extends T> factory, Block... blocks)
    {
        return new FabricBlockEntityTypeBuilder<>(factory, blocks);
    }

    public BlockEntityType<T> build()
    {
        return BlockEntityType.Builder.<T>create(this.factory, this.blocks).build(null);
    }
}
