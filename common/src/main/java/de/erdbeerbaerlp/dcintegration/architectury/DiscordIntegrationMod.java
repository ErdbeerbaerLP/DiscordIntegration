package de.erdbeerbaerlp.dcintegration.architectury;

import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.architectury.api.ArchitecturyDiscordEventHandler;
import de.erdbeerbaerlp.dcintegration.architectury.command.McCommandDiscord;
import de.erdbeerbaerlp.dcintegration.architectury.metrics.Metrics;
import de.erdbeerbaerlp.dcintegration.architectury.util.SerializeComponentUtils;
import de.erdbeerbaerlp.dcintegration.architectury.util.MessageUtilsImpl;
import de.erdbeerbaerlp.dcintegration.architectury.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.addon.AddonLoader;
import de.erdbeerbaerlp.dcintegration.common.addon.DiscordAddonMeta;
import de.erdbeerbaerlp.dcintegration.common.storage.CommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;
import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.LOGGER;

public final class DiscordIntegrationMod {
    public static final String MOD_ID = "dcintegration";
    public static MinecraftServer server = null;
    public static Metrics bstats;
    public static boolean stopped = false;


    public static final ArrayList<UUID> timeouts = new ArrayList<>();

    public static void init() {
        try {
            DiscordIntegration.loadConfigs();
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Config loading failed");
            e.printStackTrace();
        } catch (IllegalStateException e) {
            DiscordIntegration.LOGGER.error("Failed to read config file! Please check your config file!\nError description: " + e.getMessage());
            DiscordIntegration.LOGGER.error("\nStacktrace: ");
            e.printStackTrace();
        }
    }

    public static void serverStarting(MinecraftServer minecraftServer) {
        server = minecraftServer;
        DiscordIntegration.INSTANCE = new DiscordIntegration(new ServerInterface());
        try {
            //Wait a short time to allow JDA to get initiaized
            DiscordIntegration.LOGGER.info("Waiting for JDA to initialize to send starting message... (max 5 seconds before skipping)");
            for (int i = 0; i <= 5; i++) {
                if (DiscordIntegration.INSTANCE.getJDA() == null) Thread.sleep(1000);
                else break;
            }
            if (DiscordIntegration.INSTANCE.getJDA() != null) {
                Thread.sleep(2000); //Wait for it to cache the channels
                CommandRegistry.registerDefaultCommands();
                if (!Localization.instance().serverStarting.isEmpty()) {

                    if (!Localization.instance().serverStarting.isBlank())
                        if (DiscordIntegration.INSTANCE.getChannel() != null) {
                            final MessageCreateData m;
                            if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed)
                                m = new MessageCreateBuilder().setEmbeds(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarting).build()).build();
                            else
                                m = new MessageCreateBuilder().addContent(Localization.instance().serverStarting).build();
                            DiscordIntegration.startingMsg = DiscordIntegration.INSTANCE.sendMessageReturns(m, DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                        }
                }
            }
        } catch (InterruptedException | NullPointerException ignored) {
        }
        new McCommandDiscord(minecraftServer.getCommands().getDispatcher());
    }

    public static void serverStarted(MinecraftServer minecraftServer) {
        DiscordIntegration.LOGGER.info("Started");
        if (DiscordIntegration.INSTANCE != null) {
            DiscordIntegration.started = new Date().getTime();
            if (!Localization.instance().serverStarted.isBlank())
                if (DiscordIntegration.startingMsg != null) {
                    if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed) {
                        if (!Configuration.instance().embedMode.startMessages.customJSON.isBlank()) {
                            final EmbedBuilder b = Configuration.instance().embedMode.startMessages.toEmbedJson(Configuration.instance().embedMode.startMessages.customJSON);
                            DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessageEmbeds(b.build()).queue());
                        } else
                            DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessageEmbeds(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarted).build()).queue());
                    } else
                        DiscordIntegration.startingMsg.thenAccept((a) -> a.editMessage(Localization.instance().serverStarted).queue());
                } else {
                    if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.startMessages.asEmbed) {
                        if (!Configuration.instance().embedMode.startMessages.customJSON.isBlank()) {
                            final EmbedBuilder b = Configuration.instance().embedMode.startMessages.toEmbedJson(Configuration.instance().embedMode.startMessages.customJSON);
                            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                        } else
                            DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.startMessages.toEmbed().setDescription(Localization.instance().serverStarted).build()), INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    } else
                        DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverStarted, INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                }
            DiscordIntegration.INSTANCE.startThreads();
        }
        UpdateChecker.runUpdateCheck("https://raw.githubusercontent.com/ErdbeerbaerLP/DiscordIntegration/1.21.1/update-checker.json");
        if (!DownloadSourceChecker.checkDownloadSource(new File(DiscordIntegrationMod.class.getProtectionDomain().getCodeSource().getLocation().getPath().split("%")[0]))) {
            LOGGER.warn("You likely got this mod from a third party website.");
            LOGGER.warn("Some of such websites are distributing malware or old versions.");
            LOGGER.warn("Download this mod from an official source (https://modrinth.com/plugin/dcintegration) to hide this message");
            LOGGER.warn("This warning can also be suppressed in the config file");
        }

        if (minecraftServer != null) {
            Metrics.capturedServer.set(minecraftServer);
            if (bstats == null) {
                bstats = new Metrics(9765);
            }
        }

        bstats.addCustomChart(new Metrics.DrilldownPie("addons", () -> {
            final Map<String, Map<String, Integer>> map = new HashMap<>();
            if (Configuration.instance().bstats.sendAddonStats) {  //Only send if enabled, else send empty map
                for (DiscordAddonMeta m : AddonLoader.getAddonMetas()) {
                    final Map<String, Integer> entry = new HashMap<>();
                    entry.put(m.getVersion(), 1);
                    map.put(m.getName(), entry);
                }
            }
            return map;
        }));
    }

    public static void serverStopping(MinecraftServer minecraftServer) {
        Metrics.MetricsBase.scheduler.shutdownNow();
        if (DiscordIntegration.INSTANCE != null) {
            if (!Localization.instance().serverStopped.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.stopMessages.asEmbed) {
                    if (!Configuration.instance().embedMode.stopMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.stopMessages.toEmbedJson(Configuration.instance().embedMode.stopMessages.customJSON);
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(Configuration.instance().embedMode.stopMessages.toEmbed().setDescription(Localization.instance().serverStopped).build()));
                } else
                    DiscordIntegration.INSTANCE.sendMessage(Localization.instance().serverStopped);
            DiscordIntegration.INSTANCE.stopThreads();
        }
        stopped = true;
    }

    public static void serverStopped(MinecraftServer minecraftServer) {

        Metrics.MetricsBase.scheduler.shutdownNow();
        if (DiscordIntegration.INSTANCE != null) {
            if (!stopped && DiscordIntegration.INSTANCE.getJDA() != null) minecraftServer.execute(() -> {
                DiscordIntegration.INSTANCE.stopThreads();
                if (!Localization.instance().serverCrash.isBlank())
                    try {
                        if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.stopMessages.asEmbed) {
                            DiscordIntegration.INSTANCE.sendMessageReturns(new MessageCreateBuilder().addEmbeds(Configuration.instance().embedMode.stopMessages.toEmbed().setDescription(Localization.instance().serverCrash).build()).build(), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID)).get();
                        } else
                            DiscordIntegration.INSTANCE.sendMessageReturns(new MessageCreateBuilder().setContent(Localization.instance().serverCrash).build(), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID)).get();
                    } catch (InterruptedException | ExecutionException ignored) {
                    }
            });
            DiscordIntegration.INSTANCE.kill(false);
        }
    }


    private static final Pattern mentionPattern = Pattern.compile("@([a-z0-9_.]{2,32})");
    private static final Pattern legacyMentionPattern = Pattern.compile("@(.{3,32}#[0-9]{4})");

    /**
     * Sends leave / join messages on vanish / unvanish
     */
    public static void vanish(ServerPlayer player, boolean vanished) {
        if(vanished){
            if (LinkManager.isPlayerLinked(player.getUUID()) && LinkManager.getLink(null, player.getUUID()).settings.hideFromDiscord) {
                return;
            }
            final String avatarURL = INSTANCE.getSkinURL().replace("%uuid%", player.getUUID().toString()).replace("%uuid_dashless%", player.getUUID().toString().replace("-", "")).replace("%name%", player.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
            if (INSTANCE != null && !DiscordIntegrationMod.timeouts.contains(player.getUUID())) {
                if (!Localization.instance().playerLeave.isBlank()) {
                    if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerLeaveMessages.asEmbed) {
                        if (!Configuration.instance().embedMode.playerLeaveMessages.customJSON.isBlank()) {
                            final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbedJson(Configuration.instance().embedMode.playerLeaveMessages.customJSON
                                    .replace("%uuid%", player.getUUID().toString())
                                    .replace("%uuid_dashless%", player.getUUID().toString().replace("-", ""))
                                    .replace("%name%", MessageUtilsImpl.formatPlayerName(player))
                                    .replace("%randomUUID%", UUID.randomUUID().toString())
                                    .replace("%avatarURL%", avatarURL)
                                    .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUUID()).getRGB())
                            );
                            INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                        } else {
                            final EmbedBuilder b = Configuration.instance().embedMode.playerLeaveMessages.toEmbed().setAuthor(MessageUtilsImpl.formatPlayerName(player), null, avatarURL)
                                    .setDescription(Localization.instance().playerLeave.replace("%player%", MessageUtilsImpl.formatPlayerName(player)));
                            INSTANCE.sendMessage(new DiscordMessage(b.build()),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                        }
                    } else
                        INSTANCE.sendMessage(Localization.instance().playerLeave.replace("%player%", MessageUtilsImpl.formatPlayerName(player)),INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                }
            }
        }else{
            if (LinkManager.isPlayerLinked(player.getUUID()) && LinkManager.getLink(null, player.getUUID()).settings.hideFromDiscord)
                return;
            if (!Localization.instance().playerJoin.isBlank()) {
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.playerJoinMessage.asEmbed) {
                    final String avatarURL = INSTANCE.getSkinURL().replace("%uuid%", player.getUUID().toString()).replace("%uuid_dashless%", player.getUUID().toString().replace("-", "")).replace("%name%", player.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.playerJoinMessage.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbedJson(Configuration.instance().embedMode.playerJoinMessage.customJSON
                                .replace("%uuid%", player.getUUID().toString())
                                .replace("%uuid_dashless%", player.getUUID().toString().replace("-", ""))
                                .replace("%name%", MessageUtilsImpl.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUUID()).getRGB())
                        );
                        INSTANCE.sendMessage(new DiscordMessage(b.build()));
                    } else {
                        final EmbedBuilder b = Configuration.instance().embedMode.playerJoinMessage.toEmbed();
                        b.setAuthor(MessageUtilsImpl.formatPlayerName(player), null, avatarURL)
                                .setDescription(Localization.instance().playerJoin.replace("%player%", MessageUtilsImpl.formatPlayerName(player)));
                        INSTANCE.sendMessage(new DiscordMessage(b.build()), INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
                    }
                } else
                    INSTANCE.sendMessage(Localization.instance().playerJoin.replace("%player%", MessageUtilsImpl.formatPlayerName(player)), INSTANCE.getChannel(Configuration.instance().advanced.serverChannelID));
            }
        }
    }

    public static PlayerChatMessage handleChatMessage(PlayerChatMessage message, ServerPlayer player) {
        if (DiscordIntegration.INSTANCE == null) return message;
        if(INSTANCE.getServerInterface().isPlayerVanish(player.getUUID())) return message;
        if (!((ServerInterface) DiscordIntegration.INSTANCE.getServerInterface()).playerHasPermissions(player, MinecraftPermission.SEMD_MESSAGES, MinecraftPermission.USER))
            return message;
        if (LinkManager.isPlayerLinked(player.getUUID()) && LinkManager.getLink(null, player.getUUID()).settings.hideFromDiscord) {
            return message;
        }

        final PlayerChatMessage finalMessage = message;
        final MessageEmbed embed = MessageUtilsImpl.genItemStackEmbedIfAvailable(message.decoratedContent(), player.level());
        if (DiscordIntegration.INSTANCE != null) {
            String text = message.decoratedContent().getString();
            if (DiscordIntegration.INSTANCE.callEvent((e) -> {
                if (e instanceof ArchitecturyDiscordEventHandler) {
                    return ((ArchitecturyDiscordEventHandler) e).onMcChatMessage(finalMessage.decoratedContent(), player);
                }
                return false;
            })) {
                return message;
            }
            final GuildMessageChannel channel = DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID);
            if (channel == null) {
                return message;
            }
            final String json = SerializeComponentUtils.toJson(message.decoratedContent(), player.level().registryAccess());

            final Component comp = GsonComponentSerializer.gson().deserialize(json);
            if (INSTANCE.callEvent((e) -> e.onMinecraftMessage(comp, player.getUUID()))) {
                return message;
            }

            if (!Configuration.instance().compatibility.disableParsingMentionsIngame && text.contains("@")) {
                text = mentionPattern.matcher(text).replaceAll(mr -> {
                    final String username = mr.group(1);
                    LOGGER.info(username);
                    for (Member member : INSTANCE.getChannel().getGuild().getMembersByName(username, false)) {
                        return member.getAsMention();
                    }
                    for (User user : INSTANCE.getJDA().getUsersByName(username, false)) {
                        return user.getAsMention();
                    }
                    return mr.group(0);
                });
                if (text.contains("#"))
                    text = legacyMentionPattern.matcher(text).replaceAll(mr -> {
                        final String tag = mr.group(1);
                        LOGGER.info(tag);
                        final Member member = INSTANCE.getChannel().getGuild().getMemberByTag(tag);
                        if (member != null) {
                            return member.getAsMention();
                        }
                        final User user = INSTANCE.getJDA().getUserByTag(tag);
                        if (user != null) {
                            return user.getAsMention();
                        }
                        return mr.group(0);
                    });
            }
            text = MessageUtils.escapeMarkdown(text);
            if (!Localization.instance().discordChatMessage.isBlank())
                if (Configuration.instance().embedMode.enabled && Configuration.instance().embedMode.chatMessages.asEmbed) {
                    final String avatarURL = INSTANCE.getSkinURL().replace("%uuid%", player.getUUID().toString()).replace("%uuid_dashless%", player.getUUID().toString().replace("-", "")).replace("%name%", player.getName().getString()).replace("%randomUUID%", UUID.randomUUID().toString());
                    if (!Configuration.instance().embedMode.chatMessages.customJSON.isBlank()) {
                        final EmbedBuilder b = Configuration.instance().embedMode.chatMessages.toEmbedJson(Configuration.instance().embedMode.chatMessages.customJSON
                                .replace("%uuid%", player.getUUID().toString())
                                .replace("%uuid_dashless%", player.getUUID().toString().replace("-", ""))
                                .replace("%name%", MessageUtilsImpl.formatPlayerName(player))
                                .replace("%randomUUID%", UUID.randomUUID().toString())
                                .replace("%avatarURL%", avatarURL)
                                .replace("%msg%", text)
                                .replace("%playerColor%", "" + TextColors.generateFromUUID(player.getUUID()).getRGB())
                        );
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID));
                    } else {
                        EmbedBuilder b = Configuration.instance().embedMode.chatMessages.toEmbed();
                        if (Configuration.instance().embedMode.chatMessages.generateUniqueColors)
                            b = b.setColor(TextColors.generateFromUUID(player.getUUID()));
                        b = b.setAuthor(MessageUtilsImpl.formatPlayerName(player), null, avatarURL)
                                .setDescription(text);
                        DiscordIntegration.INSTANCE.sendMessage(new DiscordMessage(b.build()), INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID));
                    }
                } else
                    DiscordIntegration.INSTANCE.sendMessage(MessageUtilsImpl.formatPlayerName(player), player.getUUID().toString(), new DiscordMessage(embed, text, true), channel);

            if (!Configuration.instance().compatibility.disableParsingMentionsIngame) {
                final String editedJson = GsonComponentSerializer.gson().serialize(MessageUtils.mentionsToNames(comp, channel.getGuild()));
                final MutableComponent txt = SerializeComponentUtils.fromJson(editedJson, player.level().registryAccess());
                message = message.withUnsignedContent(txt);
            }
        }
        return message;
    }
}
