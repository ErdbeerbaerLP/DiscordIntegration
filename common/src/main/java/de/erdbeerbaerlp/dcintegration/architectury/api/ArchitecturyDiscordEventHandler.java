package de.erdbeerbaerlp.dcintegration.architectury.api;

import de.erdbeerbaerlp.dcintegration.common.api.DiscordEventHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public abstract class ArchitecturyDiscordEventHandler extends DiscordEventHandler {
    public abstract boolean onMcChatMessage(Component txt, ServerPlayer player);
}
