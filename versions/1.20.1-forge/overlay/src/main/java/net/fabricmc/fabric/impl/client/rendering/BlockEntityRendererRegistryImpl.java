package net.fabricmc.fabric.impl.client.rendering;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;

import java.util.ArrayList;
import java.util.List;

public final class BlockEntityRendererRegistryImpl
{
    public record Entry(BlockEntityType<? extends BlockEntity> type, BlockEntityRendererFactory<?> factory)
    {}

    public static final List<Entry> PENDING = new ArrayList<>();

    public static <T extends BlockEntity> void register(BlockEntityType<? extends T> type, BlockEntityRendererFactory<T> factory)
    {
        PENDING.add(new Entry(type, factory));
    }
}
