package de.erdbeerbaerlp.dcintegration.architectury.fabric;

import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class DiscordIntegrationFabric implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        DiscordIntegrationMod.init();
        if (!Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) {
            ServerLifecycleEvents.SERVER_STARTED.register(DiscordIntegrationMod::serverStarted);
            ServerLifecycleEvents.SERVER_STARTING.register(DiscordIntegrationMod::serverStarting);
            ServerLifecycleEvents.SERVER_STOPPED.register(DiscordIntegrationMod::serverStopped);
            ServerLifecycleEvents.SERVER_STOPPING.register(DiscordIntegrationMod::serverStopping);
        } else {
            DiscordIntegration.LOGGER.error("Please check the config file and set an bot token");
        }
    }
}
