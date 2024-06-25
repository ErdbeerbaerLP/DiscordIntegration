package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.util.ArchitecturyMessageUtils;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.TextColors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;

@Mixin(PlayerAdvancements.class)
public class AdvancementMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/PlayerAdvancements;markForVisibilityUpdate(Lnet/minecraft/advancements/AdvancementHolder;)V"))
    public void advancement(AdvancementHolder advancementEntry, String criterionName, CallbackInfoReturnable<Boolean> cir) {
        if (DiscordIntegration.INSTANCE == null) return;
        final Advancement advancement = advancementEntry.value();
        if (LinkManager.isPlayerLinked(player.getUUID()) && LinkManager.getLink(null, player.getUUID()).settings.hideFromDiscord)
            return;
        if (advancement != null && advancement.display().isPresent() && advancement.display().get().shouldAnnounceChat()) {

            if (!Localization.instance().advancementMessage.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.advancementMessage.asEmbed) {
                    final String avatarURL = Configuration.instance().webhook.playerAvatarURL.replace("%uuid%", player.getUUID().toString()).replace("%uuid_dashless%", player.getUUID().toString().replace("-", "")).replace("%name%", player.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.advancementMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.advancementMessage.toEmbedJson(Configuration.instance().embedMode.advancementMessage.customJSON
                                .replace("%uuid%", player.getUUID().toString())
                                .replace("%uuid_dashless%", player.getUUID().toString().replace("-", ""))
                                .replace("%name%", ArchitecturyMessageUtils.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%advName%", ChatFormatting.stripFormatting(advancement.display().get().getTitle().getString()))
                                .replace("%advDesc%", ChatFormatting.stripFormatting(advancement.display().get().getDescription().getString()))
                                .replace("%advNameURL%", URLEncoder.encode(ChatFormatting.stripFormatting(advancement.display().get().getTitle().getString()), StandardCharsets.UTF_8))
                                .replace("%advDescURL%", URLEncoder.encode(ChatFormatting.stripFormatting(advancement.display().get().getDescription().getString()), StandardCharsets.UTF_8))
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUUID()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.advancementMessage.toEmbed();
                        b = b.setAuthor(ArchitecturyMessageUtils.formatPlayerName(player), null, avatarURL)
                                .setDescription(Localization.instance().advancementMessage.replace("%player%",
                                                ChatFormatting.stripFormatting(ArchitecturyMessageUtils.formatPlayerName(player)))
                                        .replace("%advName%",
                                                ChatFormatting.stripFormatting(advancement
                                                        .display().get()
                                                        .getTitle()
                                                        .getString()))
                                        .replace("%advDesc%",
                                                ChatFormatting.stripFormatting(advancement
                                                        .display().get()
                                                        .getDescription()
                                                        .getString()))
                                        .replace("\\n", "\n")
                                        .replace("%advNameURL%", URLEncoder.encode(ChatFormatting.stripFormatting(advancement.display().get().getTitle().getString()), StandardCharsets.UTF_8))
                                        .replace("%advDescURL%", URLEncoder.encode(ChatFormatting.stripFormatting(advancement.display().get().getDescription().getString()), StandardCharsets.UTF_8))
                                );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().advancementMessage.replace("%player%",
                                    ChatFormatting.stripFormatting(ArchitecturyMessageUtils.formatPlayerName(player)))
                            .replace("%advName%",
                                    ChatFormatting.stripFormatting(advancement
                                            .display().get()
                                            .getTitle()
                                            .getString()))
                            .replace("%advDesc%",
                                    ChatFormatting.stripFormatting(advancement
                                            .display().get()
                                            .getDescription()
                                            .getString()))
                            .replace("%advNameURL%", URLEncoder.encode(ChatFormatting.stripFormatting(advancement.display().get().getTitle().getString()), StandardCharsets.UTF_8))
                            .replace("%advDescURL%", URLEncoder.encode(ChatFormatting.stripFormatting(advancement.display().get().getDescription().getString()), StandardCharsets.UTF_8))
                            .replace("\\n", "\n"),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
        }


    }
}
