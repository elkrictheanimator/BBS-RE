package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class PlayerBlockBreakEvents
{
    public static final Event<After> AFTER = EventFactory.createArrayBacked(After.class, (callbacks) -> (world, player, pos, state, blockEntity) ->
    {
        for (After callback : callbacks)
        {
            callback.afterBlockBreak(world, player, pos, state, blockEntity);
        }
    });

    public interface After
    {
        void afterBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, BlockEntity blockEntity);
    }
}
