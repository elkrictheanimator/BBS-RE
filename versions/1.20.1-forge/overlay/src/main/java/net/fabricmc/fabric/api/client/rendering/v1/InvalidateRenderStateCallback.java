package net.fabricmc.fabric.api.client.rendering.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface InvalidateRenderStateCallback
{
    Event<InvalidateRenderStateCallback> EVENT = EventFactory.createArrayBacked(InvalidateRenderStateCallback.class, (callbacks) -> () ->
    {
        for (InvalidateRenderStateCallback callback : callbacks)
        {
            callback.onInvalidate();
        }
    });

    void onInvalidate();
}
