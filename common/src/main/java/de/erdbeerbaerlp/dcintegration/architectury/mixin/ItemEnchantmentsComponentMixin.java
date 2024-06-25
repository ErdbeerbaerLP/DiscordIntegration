package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.accessors.ShowInTooltipAccessor;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemEnchantments.class)
public class ItemEnchantmentsComponentMixin implements ShowInTooltipAccessor {
    @Shadow @Final boolean showInTooltip;


    public boolean discordIntegration$showsInTooltip() {
        return showInTooltip;
    }
}
