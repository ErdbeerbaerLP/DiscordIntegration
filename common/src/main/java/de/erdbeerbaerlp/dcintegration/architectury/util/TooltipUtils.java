package de.erdbeerbaerlp.dcintegration.architectury.util;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.core.component.DataComponents;

public class TooltipUtils {
    public static boolean showsInTooltip(ItemStack stack) {
        TooltipDisplay tooltip = stack.get(DataComponents.TOOLTIP_DISPLAY);
        return tooltip == null || tooltip == TooltipDisplay.DEFAULT;
    }
}