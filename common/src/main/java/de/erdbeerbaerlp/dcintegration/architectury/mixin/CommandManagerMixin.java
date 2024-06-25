package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dcshadow.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import de.erdbeerbaerlp.dcintegration.architectury.util.ArchitecturyServerInterface;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.storage.Localization;
import de.erdbeerbaerlp.dcintegration.common.util.DiscordMessage;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.ArrayUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Arrays;
import java.util.regex.Pattern;

@Mixin(Commands.class)
public class CommandManagerMixin {

    @Inject(method = "performCommand", cancellable = true, at = @At("HEAD"))
    public void execute(ParseResults<CommandSourceStack> parseResults, String command, CallbackInfo ci) {
        final CommandSourceStack source = parseResults.getContext().getSource();
        String name = source.getTextName();
        command = command.replaceFirst(Pattern.quote("/"), "");
        if (DiscordIntegration.INSTANCE != null) {
            if (!Configuration.instance().commandLog.channelID.equals("0")) {
                if ((!Configuration.instance().commandLog.commandWhitelist && !ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0])) ||
                        (Configuration.instance().commandLog.commandWhitelist && ArrayUtils.contains(Configuration.instance().commandLog.ignoredCommands, command.split(" ")[0])))
                    DiscordIntegration.INSTANCE.sendMessage(Configuration.instance().commandLog.message
                            .replace("%sender%", name)
                            .replace("%cmd%", command)
                            .replace("%cmd-no-args%", command.split(" ")[0]), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().commandLog.channelID));
            }
            boolean raw = false;
            if (((command.startsWith("say")) && Configuration.instance().messages.sendOnSayCommand) || (command.startsWith("me") && Configuration.instance().messages.sendOnMeCommand)) {
                String msg = command.replace("say ", "");
                if (command.startsWith("say"))
                    msg = msg.replaceFirst("say ", "");
                if (command.startsWith("me")) {
                    raw = true;
                    msg = "*" + MessageUtils.escapeMarkdown(msg.replaceFirst("me ", "").trim()) + "*";
                }


                if(Configuration.instance().webhook.enable && name.equals("Rcon") && Configuration.instance().webhook.useServerNameForRcon){
                    name = Configuration.instance().webhook.serverName;
                }else if(Configuration.instance().webhook.enable && name.equals("Server") && Configuration.instance().webhook.useServerNameForConsole){
                    name = Configuration.instance().webhook.serverName;
                }
                final Entity sourceEntity = source.getEntity();

                DiscordIntegration.INSTANCE.sendMessage(name, sourceEntity != null ? sourceEntity.getUUID().toString() : "0000000", new DiscordMessage(null, msg, !raw), DiscordIntegration.INSTANCE.getChannel(Configuration.instance().advanced.chatOutputChannelID));
            }

            if (command.startsWith("discord ") || command.startsWith("dc ")) {
                final String[] args = command.replace("discord ", "").replace("dc ", "").split(" ");
                for (MCSubCommand mcSubCommand : McCommandRegistry.getCommands()) {
                    if (args[0].equals(mcSubCommand.getName())) {
                        final String[] cmdArgs = args.length > 1 ? Arrays.copyOfRange(args, 1, args.length) : new String[0];
                        switch (mcSubCommand.getType()) {
                            case CONSOLE_ONLY:
                                try {
                                    source.getPlayerOrException();
                                    source.sendFailure(Component.literal(Localization.instance().commands.consoleOnly));
                                } catch (CommandSyntaxException e) {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));
                                    source.sendSuccess(() -> Component.Serializer.fromJson(txt, VanillaRegistries.createLookup()), false);
                                }
                                break;
                            case PLAYER_ONLY:
                                try {
                                    final ServerPlayer player = source.getPlayerOrException();
                                    if (!mcSubCommand.needsOP() && ((ArchitecturyServerInterface) DiscordIntegration.INSTANCE.getServerInterface()).playerHasPermissions(player, MinecraftPermission.RUN_DISCORD_COMMAND, MinecraftPermission.USER)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> Component.Serializer.fromJson(txt, player.level().registryAccess()), false);
                                    } else if (((ArchitecturyServerInterface) DiscordIntegration.INSTANCE.getServerInterface()).playerHasPermissions(player, MinecraftPermission.RUN_DISCORD_COMMAND_ADMIN)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> Component.Serializer.fromJson(txt, player.level().registryAccess()), false);
                                    } else if (source.hasPermission(4)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> Component.Serializer.fromJson(txt, player.level().registryAccess()), false);
                                    } else {
                                        source.sendFailure(Component.literal(Localization.instance().commands.noPermission));
                                    }
                                } catch (CommandSyntaxException e) {
                                    source.sendFailure(Component.literal(Localization.instance().commands.ingameOnly));

                                }
                                break;
                            case BOTH:
                                try {
                                    final ServerPlayer player = source.getPlayerOrException();
                                    if (!mcSubCommand.needsOP() && ((ArchitecturyServerInterface) DiscordIntegration.INSTANCE.getServerInterface()).playerHasPermissions(player, MinecraftPermission.RUN_DISCORD_COMMAND, MinecraftPermission.USER)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> Component.Serializer.fromJson(txt, VanillaRegistries.createLookup()), false);
                                    } else if (((ArchitecturyServerInterface) DiscordIntegration.INSTANCE.getServerInterface()).playerHasPermissions(player, MinecraftPermission.RUN_DISCORD_COMMAND_ADMIN)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> Component.Serializer.fromJson(txt, VanillaRegistries.createLookup()), false);
                                    } else if (source.hasPermission(4)) {
                                        final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, player.getUUID()));
                                        source.sendSuccess(() -> Component.Serializer.fromJson(txt, VanillaRegistries.createLookup()), false);
                                    } else {
                                        source.sendFailure(Component.literal(Localization.instance().commands.noPermission));
                                    }
                                } catch (CommandSyntaxException e) {
                                    final String txt = GsonComponentSerializer.gson().serialize(mcSubCommand.execute(cmdArgs, null));
                                    source.sendSuccess(() -> Component.Serializer.fromJson(txt, VanillaRegistries.createLookup()), false);
                                }
                                break;
                        }
                    }
                }
                ci.cancel();
            }
        }
    }
}
