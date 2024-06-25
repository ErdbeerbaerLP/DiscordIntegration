package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.ArchitecturyMessageUtils;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(at = @At(value = "TAIL"), method = "die")
    private void onPlayerDeath(DamageSource s, CallbackInfo info) {
        ServerPlayer p = (ServerPlayer) (Object) this;

        if (DiscordIntegration.INSTANCE != null) {
            if (LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.hideFromDiscord)
                return;
            final Component deathMessage = s.getLocalizedDeathMessage(p);
            final MessageEmbed embed = ArchitecturyMessageUtils.genItemStackEmbedIfAvailable(deathMessage, p.level());
            if (!Localization.instance().playerDeath.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.deathMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", p.getUUID().toString()).replace("%uuid_dashless%", p.getUUID().toString().replace("-", "")).replace("%name%", p.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if(!Configuration.instance().embedMode.deathMessage.customJSON.isBlank()){
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbedJson(Configuration.instance().embedMode.deathMessage.customJSON
                                .replace("%uuid%", p.getUUID().toString())
                                .replace("%uuid_dashless%", p.getUUID().toString().replace("-", ""))
                                .replace("%name%", ArchitecturyMessageUtils.formatPlayerName(p))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%deathMessage%", ChatFormatting.stripFormatting(deathMessage.getString()).replace(ArchitecturyMessageUtils.formatPlayerName(p) + " ", ""))
                                .replace("%playerColor%", ""+ TextColors.generateFromUUID(p.getUUID()).getRGB())
                        );
                        if (embed != null) {
                            b.addBlankField(false);
                            b.addField(embed.getTitle() + " *(" + embed.getFooter().getText() + ")*", embed.getDescription(), false);
                        }
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
                    }else {
                        final EmbedBuilder b = Configuration.instance().embedMode.deathMessage.toEmbed();
                        b.setDescription(":skull: " + Localization.instance().playerDeath.replace("%player%", ArchitecturyMessageUtils.formatPlayerName(p)).replace("%msg%", ChatFormatting.stripFormatting(deathMessage.getString()).replace(ArchitecturyMessageUtils.formatPlayerName(p) + " ", "")));
                        if (embed != null) {
                            b.addBlankField(false);
                            b.addField(embed.getTitle() + " *(" + embed.getFooter().getText() + ")*", embed.getDescription(), false);
                        }
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(embed, Localization.instance().playerDeath.replace("%player%", ArchitecturyMessageUtils.formatPlayerName(p)).replace("%msg%",  ChatFormatting.stripFormatting(deathMessage.getString()).replace(ArchitecturyMessageUtils.formatPlayerName(p) + " ", ""))), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.deathsChannelID));
        }
    }
}
