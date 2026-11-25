package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface AttackBlockCallback
{
    Event<AttackBlockCallback> EVENT = EventFactory.createArrayBacked(AttackBlockCallback.class, (listeners) -> (player, world, hand, pos, direction) ->
    {
        for (AttackBlockCallback listener : listeners)
        {
            ActionResult result = listener.interact(player, world, hand, pos, direction);

            if (result != ActionResult.PASS)
            {
                return result;
            }
        }

        return ActionResult.PASS;
    });

    ActionResult interact(PlayerEntity player, World world, Hand hand, BlockPos pos, Direction direction);
}
