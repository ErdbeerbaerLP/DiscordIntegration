package de.erdbeerbaerlp.dcintegration.architectury.forge;

import de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.util.HashMap;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.LOGGER;

@Mod(DiscordIntegrationMod.MOD_ID)
public final class DiscordIntegrationForge {
    public static final HashMap<String, PermissionNode<Boolean>> nodes = new HashMap<>();
    private static MinecraftServer currentServer = null;

    public DiscordIntegrationForge() {
        DiscordIntegrationMod.init();
        if (Configuration.instance().general.botToken.equals("INSERT BOT TOKEN HERE")) {
            LOGGER.error("Please check the config file and set a bot token");
        } else {
            MinecraftForge.EVENT_BUS.register(this);
        }
    }

    @SubscribeEvent
    public void serverSetup(final FMLDedicatedServerSetupEvent ev) {
    }

    @SubscribeEvent
    public void serverStarting(final ServerStartingEvent ev) {
        DiscordIntegrationMod.serverStarting(ev.getServer());
        if (ModList.get().getModContainerById("dynmap").isPresent()) {
            new DynmapListener().register();
        }
    }

    @SubscribeEvent
    public void serverStarted(final ServerStartedEvent ev) {
        currentServer = ev.getServer();
        DiscordIntegrationMod.serverStarted(ev.getServer());
    }

    @SubscribeEvent
    public void serverStopping(final ServerStoppingEvent ev) {
        DiscordIntegrationMod.serverStopping(ev.getServer());
    }

    @SubscribeEvent
    public void serverStopped(final ServerStoppedEvent ev) {
        currentServer = null;
        DiscordIntegrationMod.serverStopped(ev.getServer());
    }

    @SubscribeEvent
    public void addPermissions(final PermissionGatherEvent.Nodes ev) {
        for (MinecraftPermission p : MinecraftPermission.values()) {
            nodes.put(p.getAsString(), new PermissionNode<>("dcintegration", p.getAsString().replace("dcintegration.", ""), PermissionTypes.BOOLEAN, (player, playerUUID, context) -> p.getDefaultValue()));
        }
        ev.addNodes(nodes.values().toArray(new PermissionNode[0]));
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }
}