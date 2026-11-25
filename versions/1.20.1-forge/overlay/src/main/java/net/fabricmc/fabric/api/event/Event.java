package net.fabricmc.fabric.api.event;

import net.minecraft.util.Identifier;

public abstract class Event<T>
{
    public static final Identifier DEFAULT_PHASE = new Identifier("fabric", "default");

    protected volatile T invoker;

    public final T invoker()
    {
        return this.invoker;
    }

    public abstract void register(T listener);

    public void register(Identifier phase, T listener)
    {
        this.register(listener);
    }

    public void addPhaseOrdering(Identifier first, Identifier second)
    {}
}
