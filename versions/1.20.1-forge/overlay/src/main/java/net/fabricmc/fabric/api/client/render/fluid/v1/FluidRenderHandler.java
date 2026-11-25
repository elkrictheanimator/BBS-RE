package net.fabricmc.fabric.api.client.render.fluid.v1;

import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public interface FluidRenderHandler
{
    Sprite[] getFluidSprites(BlockRenderView view, BlockPos pos, FluidState state);
}
