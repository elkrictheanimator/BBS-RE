package net.fabricmc.fabric.api.client.item.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public interface ItemTooltipCallback
{
    Event<ItemTooltipCallback> EVENT = EventFactory.createArrayBacked(ItemTooltipCallback.class, (callbacks) -> (stack, context, lines) ->
    {
        for (ItemTooltipCallback callback : callbacks)
        {
            callback.getTooltip(stack, context, lines);
        }
    });

    void getTooltip(ItemStack stack, TooltipContext context, List<Text> lines);
}
