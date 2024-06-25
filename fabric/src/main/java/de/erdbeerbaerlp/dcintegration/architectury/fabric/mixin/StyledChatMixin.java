package de.erdbeerbaerlp.dcintegration.architectury.fabric.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import eu.pb4.styledchat.StyledChatUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(StyledChatUtils.class)
public class StyledChatMixin {
    @Redirect(method = "modifyForSending", at = @At(value = "INVOKE", target = "Leu/pb4/styledchat/StyledChatUtils;formatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/network/chat/Component;"))
    private static Component message(PlayerChatMessage msg, CommandSourceStack s, ResourceKey<ChatType> e) {
        if (e.equals(ChatType.CHAT))
            msg = DiscordIntegrationMod.handleChatMessage(msg, s.getPlayer());
        return StyledChatUtils.formatMessage(msg, s, e);
    }
}
