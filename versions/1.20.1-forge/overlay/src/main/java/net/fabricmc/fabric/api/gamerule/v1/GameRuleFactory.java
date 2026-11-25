package net.fabricmc.fabric.api.gamerule.v1;

import net.minecraft.world.GameRules;

public final class GameRuleFactory
{
    public static GameRules.Type<GameRules.BooleanRule> createBooleanRule(boolean defaultValue)
    {
        return GameRules.BooleanRule.create(defaultValue);
    }
}
