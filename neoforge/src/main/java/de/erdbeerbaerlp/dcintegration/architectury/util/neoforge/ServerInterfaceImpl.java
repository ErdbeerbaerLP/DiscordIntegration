package de.erdbeerbaerlp.dcintegration.architectury.util.neoforge;

import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import de.erdbeerbaerlp.dcintegration.architectury.neoforge.DiscordIntegrationForge;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import redstonedubstep.mods.vanishmod.VanishUtil;

import java.util.UUID;

public class ServerInterfaceImpl {
    public static String getLoaderNameX() {
        return "Forge";
    }

    public static String getLoaderVersion() {
        return FMLLoader.getCurrent().getVersionInfo() + " (MC: " + FMLLoader.getCurrent().getVersionInfo().mcVersion() + ")";
    }

    public static boolean playerHasPermissionsX(UUID player, String... permissions) {
        final ServerPlayer serverPlayer = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(player);
        for (String p : permissions) {
            if (serverPlayer != null) {
                if (PermissionAPI.getPermission(serverPlayer, DiscordIntegrationForge.nodes.get(p))) {
                    return true;
                }
            } else {
                if (PermissionAPI.getOfflinePermission(player, DiscordIntegrationForge.nodes.get(p))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean playerHasPermissionsX(Player serverPlayer, String... permissions) {
        for (String p : permissions) {
            if (PermissionAPI.getPermission((ServerPlayer) serverPlayer, DiscordIntegrationForge.nodes.get(p))) {
                return true;
            }
        }
        return false;
    }

    public static boolean checkVanish(UUID player) {
        if (ModList.get().isLoaded("vmod")) {
            final ServerPlayer p = DiscordIntegrationMod.server.getPlayerList().getPlayer(player);
            if (p != null)
                if(VanishUtil.isVanished(p)) return VanishUtil.isVanished(p);
        }
        return false;
    }
}
