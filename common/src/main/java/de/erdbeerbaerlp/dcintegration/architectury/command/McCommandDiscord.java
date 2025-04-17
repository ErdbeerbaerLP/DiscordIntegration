package de.erdbeerbaerlp.dcintegration.architectury.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.architectury.util.ServerInterface;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.MCSubCommand;
import de.erdbeerbaerlp.dcintegration.common.minecraftCommands.McCommandRegistry;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import java.net.URI;

public class McCommandDiscord {
    public McCommandDiscord(CommandDispatcher<CommandSourceStack> dispatcher) {
        final LiteralArgumentBuilder<CommandSourceStack> l = Commands.literal("discord");

        if (Configuration.instance().ingameCommand.enabled) {
            l.executes((ctx) -> {
                MutableComponent base = Component.literal(Configuration.instance().ingameCommand.message);
                MutableComponent hover = Component.literal(Configuration.instance().ingameCommand.hoverMessage);
                URI url = URI.create(Configuration.instance().ingameCommand.inviteURL);

                MutableComponent full = base.withStyle(style -> style
                        .withClickEvent(new ClickEvent.OpenUrl(url))
                        .withHoverEvent(new HoverEvent.ShowText(hover))
                );

                ctx.getSource().sendSuccess(() -> full, false);
                return 0;
            }).requires((s) -> {
                try {
                    return ((ServerInterface) DiscordIntegration.INSTANCE.getServerInterface())
                            .playerHasPermissions(s.getPlayerOrException(), MinecraftPermission.USER, MinecraftPermission.RUN_DISCORD_COMMAND);
                } catch (CommandSyntaxException e) {
                    return true;
                }
            });
        }

        for (final MCSubCommand cmd : McCommandRegistry.getCommands()) {
            l.then(Commands.literal(cmd.getName()));
        }

        dispatcher.register(l);
    }
}
