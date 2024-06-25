package de.erdbeerbaerlp.dcintegration.architectury.neoforge;

import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;

import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.HashMap;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.LOGGER;

@Mod(DiscordIntegrationMod.MOD_ID)
public final class DiscordIntegrationForge {
    public DiscordIntegrationForge(IEventBus modEventBus) {
        DiscordIntegrationMod.init();
        if (Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) { //Prevent events when token not set or on client
            LOGGER.error("Please check the config file and set an bot token");
        } else {
            modEventBus.addListener(this::serverSetup);
            NeoForge.EVENT_BUS.register(this);
        }
    }
    public void serverSetup(FMLDedicatedServerSetupEvent ev) {

    }
    @SubscribeEvent
    public void serverStarting(final ServerStartingEvent ev) {
        DiscordIntegrationMod.serverStarting(ev.getServer());
    }
    @SubscribeEvent
    public void serverStarted(final ServerStartedEvent ev) {
        DiscordIntegrationMod.serverStarted(ev.getServer());

    }
    @SubscribeEvent
    public void serverStopping(final ServerStoppingEvent ev) {
        DiscordIntegrationMod.serverStopping(ev.getServer());

    }
    @SubscribeEvent
    public void serverStopped(final ServerStoppedEvent ev) {
        DiscordIntegrationMod.serverStopped(ev.getServer());
    }
    public static final HashMap<String, PermissionNode<Boolean>> nodes = new HashMap();

    @SubscribeEvent
    public void addPermissions(final PermissionGatherEvent.Nodes ev) {
        for (MinecraftPermission p : MinecraftPermission.values()) {
            nodes.put(p.getAsString(), new PermissionNode<>("dcintegration", p.getAsString().replace("dcintegration.", ""), PermissionTypes.BOOLEAN, (player, playerUUID, context) -> p.getDefaultValue()));
        }
        ev.addNodes(nodes.values().toArray(new PermissionNode[0]));
    }
}
