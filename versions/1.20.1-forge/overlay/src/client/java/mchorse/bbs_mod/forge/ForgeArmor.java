package mchorse.bbs_mod.forge;

import mchorse.bbs_mod.client.renderer.entity.ActorEntityRenderer;
import mchorse.bbs_mod.cubic.render.vanilla.ArmorRenderer;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;

/**
 * The armor renderer is normally built when the actor's entity renderer is constructed. On Forge
 * (and with mods that build entity renderers lazily) the form picker can draw a model before that
 * ever happened, so build it on first use instead.
 */
public class ForgeArmor
{
    public static ArmorRenderer get()
    {
        if (ActorEntityRenderer.armorRenderer == null)
        {
            MinecraftClient mc = MinecraftClient.getInstance();
            EntityModelLoader loader = mc.getEntityModelLoader();

            ActorEntityRenderer.armorRenderer = new ArmorRenderer(
                new ArmorEntityModel(loader.getModelPart(EntityModelLayers.PLAYER_INNER_ARMOR)),
                new ArmorEntityModel(loader.getModelPart(EntityModelLayers.PLAYER_OUTER_ARMOR)),
                loader.getModelPart(EntityModelLayers.ELYTRA),
                mc.getBakedModelManager()
            );
        }

        return ActorEntityRenderer.armorRenderer;
    }
}
