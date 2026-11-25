package mchorse.bbs_mod.forge;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraftforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stands in for {@code Registry.register}: Forge locks the vanilla registries outside its own register
 * event, so the entries are queued here and handed over when the event for their registry fires.
 */
public class ForgeRegistry
{
    private record Entry(Identifier id, Object value)
    {}

    private static final Map<Identifier, List<Entry>> QUEUE = new HashMap<>();

    public static <V, T extends V> T register(Registry<V> registry, Identifier id, T entry)
    {
        QUEUE.computeIfAbsent(registry.getKey().getValue(), (k) -> new ArrayList<>()).add(new Entry(id, entry));

        return entry;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void flush(RegisterEvent event)
    {
        List<Entry> list = QUEUE.remove(event.getRegistryKey().getValue());

        if (list == null)
        {
            return;
        }

        for (Entry entry : list)
        {
            event.register((RegistryKey) event.getRegistryKey(), entry.id, () -> entry.value);
        }
    }
}
