package net.fabricmc.fabric.api.client.keybinding.v1;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public final class KeyBindingHelper
{
    /** Collected here, handed to Forge's RegisterKeyMappingsEvent. */
    public static final List<KeyBinding> PENDING = new ArrayList<>();

    public static KeyBinding registerKeyBinding(KeyBinding binding)
    {
        PENDING.add(binding);

        return binding;
    }

    public static InputUtil.Key getBoundKeyOf(KeyBinding binding)
    {
        return binding.getKey();
    }
}
