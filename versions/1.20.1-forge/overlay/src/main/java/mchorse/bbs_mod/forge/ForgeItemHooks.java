package mchorse.bbs_mod.forge;

import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** Gives the mod's items the Forge client extension that points at their dynamic renderer. */
public class ForgeItemHooks
{
    private static BuiltinModelItemRenderer renderer;

    public static IClientItemExtensions extensions()
    {
        return new IClientItemExtensions()
        {
            @Override
            public BuiltinModelItemRenderer getCustomRenderer()
            {
                if (renderer == null)
                {
                    MinecraftClient mc = MinecraftClient.getInstance();

                    renderer = new BuiltinModelItemRenderer(mc.getBlockEntityRenderDispatcher(), mc.getEntityModelLoader())
                    {
                        @Override
                        public void render(ItemStack stack, ModelTransformationMode mode, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay)
                        {
                            BuiltinItemRendererRegistry.DynamicItemRenderer dynamic = BuiltinItemRendererRegistry.RENDERERS.get(stack.getItem());

                            if (dynamic != null)
                            {
                                dynamic.render(stack, mode, matrices, vertexConsumers, light, overlay);
                            }
                        }
                    };
                }

                return renderer;
            }
        };
    }
}
