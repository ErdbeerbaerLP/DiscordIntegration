package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;


@Mixin(value= ServerGamePacketListenerImpl.class)
public class ChatMixin {
    /**
     * Handle chat messages
     */
    @Redirect(method = "broadcastChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V"))
    public void chatMessage(PlayerList instance, PlayerChatMessage signedMessage, ServerPlayer sender, ChatType.Bound bound) {
        signedMessage = DiscordIntegrationMod.handleChatMessage(signedMessage, sender);
        instance.broadcastChatMessage( signedMessage, sender, bound);

    }
}
