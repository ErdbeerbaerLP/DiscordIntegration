package de.erdbeerbaerlp.dcintegration.architectury.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.com.vdurmont.emoji.EmojiParser;
import dcshadow.net.kyori.adventure.text.Component;
import dcshadow.net.kyori.adventure.text.TextReplacementConfig;
import dcshadow.net.kyori.adventure.text.event.ClickEvent;
import dcshadow.net.kyori.adventure.text.event.HoverEvent;
import dcshadow.net.kyori.adventure.text.format.Style;
import dcshadow.net.kyori.adventure.text.format.TextColor;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import dcshadow.net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import de.erdbeerbaerlp.dcintegration.architectury.command.DCCommandSender;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.storage.linking.LinkManager;
import de.erdbeerbaerlp.dcintegration.common.util.ComponentUtils;
import de.erdbeerbaerlp.dcintegration.common.util.McServerInterface;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.requests.RestAction;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import static de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod.server;

public class ArchitecturyServerInterface implements McServerInterface{
    @Override
    public int getMaxPlayers() {
        return server.getMaxPlayers();
    }

    @Override
    public int getOnlinePlayers() {
        return server.getPlayerCount();
    }

    @Override
    public void sendIngameMessage(Component msg) {
        final List<ServerPlayer> l = server.getPlayerList().getPlayers();
        try {
            for (final ServerPlayer p : l) {
                if (!playerHasPermissions(p, MinecraftPermission.READ_MESSAGES, MinecraftPermission.USER))
                    return;
                if (!DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUUID()) && !(LinkManager.isPlayerLinked(p.getUUID()) && LinkManager.getLink(null, p.getUUID()).settings.ignoreDiscordChatIngame)) {
                    final Map.Entry<Boolean, Component> ping = ComponentUtils.parsePing(msg, p.getUUID(), p.getName().getString());
                    final String jsonComp = GsonComponentSerializer.gson().serialize(ping.getValue()).replace("\\\\n", "\n");
                    final net.minecraft.network.chat.Component comp = net.minecraft.network.chat.Component.Serializer.fromJson(jsonComp, p.level().registryAccess());
                    p.sendSystemMessage(comp, false);
                    if (ping.getKey()) {
                        if (LinkManager.isPlayerLinked(p.getUUID())&&LinkManager.getLink(null, p.getUUID()).settings.pingSound) {
                            p.connection.send(new ClientboundSoundPacket(SoundEvents.NOTE_BLOCK_PLING, SoundSource.MASTER, p.position().x,p.position().y,p.position().z, 1, 1, server.overworld().getSeed()));
                        }
                    }
                }
            }
            //Send to server console too
            final String jsonComp = GsonComponentSerializer.gson().serialize(msg).replace("\\\\n", "\n");
            final net.minecraft.network.chat.Component comp = net.minecraft.network.chat.Component.Serializer.fromJson(jsonComp, VanillaRegistries.createLookup());
            server.sendSystemMessage(comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendIngameReaction(Member member, RestAction<Message> retrieveMessage, UUID targetUUID, EmojiUnion reactionEmote) {
        final List<ServerPlayer> l = server.getPlayerList().getPlayers();
        for (final ServerPlayer p : l) {
            if (!playerHasPermissions(p, MinecraftPermission.READ_MESSAGES, MinecraftPermission.USER))
                return;
            if (p.getUUID().equals(targetUUID) && !DiscordIntegration.INSTANCE.ignoringPlayers.contains(p.getUUID()) && (LinkManager.isPlayerLinked(p.getUUID())&&!LinkManager.getLink(null, p.getUUID()).settings.ignoreDiscordChatIngame && !LinkManager.getLink(null, p.getUUID()).settings.ignoreReactions)) {

                final String emote = reactionEmote.getType() == Emoji.Type.UNICODE ? EmojiParser.parseToAliases(reactionEmote.getName()) : ":" + reactionEmote.getName() + ":";

                Style.Builder memberStyle = Style.style();
                if (Configuration.instance().messages.discordRoleColorIngame)
                    memberStyle = memberStyle.color(TextColor.color(member.getColorRaw()));

                final Component user = Component.text(member.getEffectiveName()).style(memberStyle
                        .clickEvent(ClickEvent.suggestCommand("<@" + member.getId() + ">"))
                        .hoverEvent(HoverEvent.showText(Component.text(Localization.instance().discordUserHover.replace("%user#tag%", member.getUser().getAsTag()).replace("%user%", member.getEffectiveName()).replace("%id%", member.getUser().getId())))));
                final TextReplacementConfig userReplacer = ComponentUtils.replaceLiteral("%user%", user);
                final TextReplacementConfig emoteReplacer = ComponentUtils.replaceLiteral("%emote%", emote);

                final Component out = LegacyComponentSerializer.legacySection().deserialize(Localization.instance().reactionMessage)
                        .replaceText(userReplacer).replaceText(emoteReplacer);

                if (Localization.instance().reactionMessage.contains("%msg%"))
                    retrieveMessage.submit().thenAccept((m) -> {
                        final String msg = MessageUtils.formatEmoteMessage(m.getMentions().getCustomEmojis(), m.getContentDisplay());
                        final TextReplacementConfig msgReplacer = ComponentUtils.replaceLiteral("%msg%", msg);
                        sendReactionMCMessage(p, out.replaceText(msgReplacer));
                    });
                else sendReactionMCMessage(p, out);
            }
        }
    }
    private void sendReactionMCMessage(ServerPlayer target, Component msgComp) {
        final String jsonComp = GsonComponentSerializer.gson().serialize(msgComp).replace("\\\\n", "\n");
        try {
            final net.minecraft.network.chat.Component comp = net.minecraft.network.chat.Component.Serializer.fromJson(jsonComp,target.level().registryAccess());
            target.sendSystemMessage(comp, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    public void runMcCommand(String cmd, CompletableFuture<InteractionHook> cmdMsg, User user) {
        final DCCommandSender s = new DCCommandSender(cmdMsg, user);
            try {
                server.getCommands().getDispatcher().execute(cmd.trim(), s.createCommandSourceStack());
            } catch (CommandSyntaxException e) {
                s.sendSystemMessage(net.minecraft.network.chat.Component.literal(e.getMessage()));
            }
    }

    @Override
    public HashMap<UUID, String> getPlayers() {
        final HashMap<UUID, String> players = new HashMap<>();
        for (final ServerPlayer p : server.getPlayerList().getPlayers()) {
            players.put(p.getUUID(), p.getDisplayName().getString().isEmpty() ? p.getName().getString() : p.getDisplayName().getString());
        }
        return players;
    }

    @Override
    public void sendIngameMessage(String msg, UUID player) {
        final ServerPlayer p = server.getPlayerList().getPlayer(player);
        if (p != null)
            p.sendSystemMessage( net.minecraft.network.chat.Component.literal(msg));
    }

    @Override
    public boolean isOnlineMode() {
        return Configuration.instance().bungee.isBehindBungee || server.usesAuthentication();
    }

    @Override
    public String getNameFromUUID(UUID uuid) {
        return server.getSessionService().fetchProfile(uuid,false).profile().getName();
    }
    @Override
    public boolean playerHasPermissions(UUID player, String... permissions) {
        return playerHasPermissionsX(player,permissions);
    }
    public boolean playerHasPermissions(Player player, String... permissions) {
        return playerHasPermissionsX(player,permissions);
    }

    @ExpectPlatform
    private static boolean playerHasPermissionsX(UUID player, String... permissions) {
        throw new AssertionError();
    }

    @Override
    public String runMCCommand(String cmd) {
        final DCCommandSender s = new DCCommandSender();
        try {
            server.getCommands().getDispatcher().execute(cmd.trim(), s.createCommandSourceStack());
            return s.message.toString();
        } catch (CommandSyntaxException e) {
            return e.getMessage();
        }
    }

    @ExpectPlatform
    private static boolean playerHasPermissionsX(Player player, String... permissions) {
        throw new AssertionError();
    }


    public boolean playerHasPermissions(Player player, MinecraftPermission... permissions) {
        final String[] permissionStrings = new String[permissions.length];
        for (int i = 0; i < permissions.length; i++) {
            permissionStrings[i] = permissions[i].getAsString();
        }
        return playerHasPermissions(player, permissionStrings);
    }


    @Override
    public String getLoaderName() {
        return getLoaderNameX();
    }

    @ExpectPlatform
    private static String getLoaderNameX() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static String getLoaderVersion() {
        throw new AssertionError();
    }
}