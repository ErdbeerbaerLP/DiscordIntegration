package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import com.mojang.authlib.GameProfile;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.architectury.util.MessageUtilsImpl;
import de.erdbeerbaerlp.dcintegration.architectury.util.SerializeComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.WorkThread;
import de.erdbeerbaerlp.dcintegration.common.compat.FloodgateUtils;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.SocketAddress;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;


@Mixin(PlayerList.class)
public class PlayerManagerMixin {

    /**
     * Handle whitelisting
     */
    @Inject(method = "canPlayerLogin", at = @At("HEAD"), cancellable = true)
    public void canJoin(SocketAddress socketAddress, NameAndId nameAndId, CallbackInfoReturnable<net.minecraft.network.chat.Component> cir) {
        if (DiscordIntegration.INSTANCE == null) return;
        LinkManager.checkGlobalAPI(nameAndId.id());
        final Component eventKick = INSTANCE.callEventO((e) -> e.onPlayerJoin(nameAndId.id()));
        if (eventKick != null) {
            final String jsonComp = GsonComponentSerializer.gson().serialize(eventKick).replace("\\\\n", "\n");
            try {
                final net.minecraft.network.chat.Component comp = SerializeComponentUtils.fromJson(jsonComp, VanillaRegistries.createLookup());
                cir.setReturnValue(comp);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (Configuration.instance().linking.whitelistMode && DiscordIntegration.INSTANCE.getServerInterface().isOnlineMode()) {
            try {
                if (!LinkManager.isPlayerLinked(nameAndId.id())) {
                    cir.setReturnValue(net.minecraft.network.chat.Component.literal(Localization.instance().linking.notWhitelistedCode.replace("%code%", "" + (FloodgateUtils.isBedrockPlayer(nameAndId.id()) ? LinkManager.genBedrockLinkNumber(nameAndId.id()) : LinkManager.genLinkNumber(nameAndId.id())))));
                } else if (!DiscordIntegration.INSTANCE.canPlayerJoin(nameAndId.id())) {
                    cir.setReturnValue(net.minecraft.network.chat.Component.literal(Localization.instance().linking.notWhitelistedRole));
                }
            } catch (IllegalStateException e) {
                cir.setReturnValue(net.minecraft.network.chat.Component.literal("An error occured\nPlease check Server Log for more information\n\n" + e));
                e.printStackTrace();
            }
        }
    }

    @Inject(at = @At(value = "TAIL"), method = "placeNewPlayer")
    private void onPlayerJoin(Connection connection, ServerPlayer p, CommonListenerCookie commonListenerCookie, CallbackInfo ci) {
        if (DiscordIntegration.INSTANCE != null) {
            if(INSTANCE.getServerInterface().isPlayerVanish(p.getUUID())) return;
            if (LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.hideFromDiscord)
                return;
            LinkManager.checkGlobalAPI(p.getUUID());
            if (!Localization.instance().playerJoin.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerJoinMessage.asEmbed) {
                    final String avatarURL = INSTANCE.getSkinURL().replace("%uuid%", p.getUUID().toString()).replace("%uuid_dashless%", p.getUUID().toString().replace("-", "")).replace("%name%", p.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.playerJoinMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbedJson(Configuration.instance().embedMode.playerJoinMessage.customJSON
                                .replace("%uuid%", p.getUUID().toString())
                                .replace("%uuid_dashless%", p.getUUID().toString().replace("-", ""))
                                .replace("%name%", MessageUtilsImpl.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(p.getUUID()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbed();
                        b.setAuthor(MessageUtilsImpl.formatPlayerName(p), null, avatarURL)
                                .setDescription(Localization.instance().playerJoin.replace("%player%", MessageUtilsImpl.formatPlayerName(p)));
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().playerJoin.replace("%player%", MessageUtilsImpl.formatPlayerName(p)), INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
            // Fix link status (if user does not have role, give the role to the user, or vice versa)
            WorkThread.executeJob(() -> {
                final UUID uuid = p.getUUID();
                if (!LinkManager.isPlayerLinked(uuid)) {
                    return;
                }

                final Member member = DiscordIntegration.INSTANCE.getMemberById(LinkManager.getLink(null, uuid).discordID);

                if (Configuration.instance().linking.shouldNickname) {
                    member.modifyNickname(MessageUtilsImpl.formatPlayerName(p)).queue();
                }

                if (Configuration.instance().linking.linkedRoleID.equals("0")) {
                    return;
                }

                final Guild guild = DiscordIntegration.INSTANCE.getChannel().getGuild();
                final Role linkedRole = guild.getRoleById(Configuration.instance().linking.linkedRoleID);

                if (!member.getRoles().contains(linkedRole)) {
                    guild.addRoleToMember(member, linkedRole).queue();
                }
            });
        }
    }
}
