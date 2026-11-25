package mchorse.bbs_mod.forge;

import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class RenderedBlockItem extends BlockItem
{
    public RenderedBlockItem(Block block, Settings settings)
    {
        super(block, settings);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer)
    {
        consumer.accept(ForgeItemHooks.extensions());
    }
}
