package net.fabricmc.fabric.api.client.render.fluid.v1;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.Sprite;
import net.minecraft.fluid.Fluid;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/** Reads the textures from the fluid's Forge client extensions, which is where modded fluids declare them. */
public final class FluidRenderHandlerRegistry
{
    public static final FluidRenderHandlerRegistry INSTANCE = new FluidRenderHandlerRegistry();

    public FluidRenderHandler get(Fluid fluid)
    {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);

        return (view, pos, state) ->
        {
            java.util.function.Function<net.minecraft.util.Identifier, Sprite> atlas = MinecraftClient.getInstance().getSpriteAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE);

            return new Sprite[] {atlas.apply(extensions.getStillTexture()), atlas.apply(extensions.getFlowingTexture())};
        };
    }
}
