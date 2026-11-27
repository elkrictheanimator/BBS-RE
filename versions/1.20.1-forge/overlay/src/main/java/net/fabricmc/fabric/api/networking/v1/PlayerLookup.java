package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class PlayerLookup
{
    /** Players that currently have the entity in range, without the entity itself. */
    public static Collection<ServerPlayerEntity> tracking(Entity entity)
    {
        if (!(entity.getWorld() instanceof ServerWorld world))
        {
            return Collections.emptyList();
        }

        Collection<ServerPlayerEntity> list = new ArrayList<>(world.getChunkManager().threadedAnvilChunkStorage.getPlayersWatchingChunk(entity.getChunkPos(), false));

        list.remove(entity);

        return list;
    }
}
