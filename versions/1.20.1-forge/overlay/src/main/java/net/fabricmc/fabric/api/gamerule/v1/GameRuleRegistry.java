package net.fabricmc.fabric.api.gamerule.v1;

import net.minecraft.world.GameRules;

public final class GameRuleRegistry
{
    public static <T extends GameRules.Rule<T>> GameRules.Key<T> register(String name, GameRules.Category category, GameRules.Type<T> type)
    {
        return GameRules.register(name, category, type);
    }
}
