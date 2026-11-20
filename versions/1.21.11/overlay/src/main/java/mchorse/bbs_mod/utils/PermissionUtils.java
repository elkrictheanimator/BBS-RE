package mchorse.bbs_mod.utils;

import mchorse.bbs_mod.BBSMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerConfigEntry;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionUtils
{
    public static boolean arePanelsAllowed(MinecraftServer server, ServerPlayerEntity player)
    {
        return BBSMod.bbsEditing || server.getPlayerManager().isOperator(new PlayerConfigEntry(player.getGameProfile()));
    }
}
