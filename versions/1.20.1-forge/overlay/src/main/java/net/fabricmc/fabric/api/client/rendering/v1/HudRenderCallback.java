package net.fabricmc.fabric.api.client.rendering.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.DrawContext;

public interface HudRenderCallback
{
    Event<HudRenderCallback> EVENT = EventFactory.createArrayBacked(HudRenderCallback.class, (callbacks) -> (context, tickDelta) ->
    {
        for (HudRenderCallback callback : callbacks)
        {
            callback.onHudRender(context, tickDelta);
        }
    });

    void onHudRender(DrawContext drawContext, float tickDelta);
}
