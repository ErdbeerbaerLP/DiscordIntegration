package de.erdbeerbaerlp.dcintegration.architectury.util.fabric;

import de.erdbeerbaerlp.dcintegration.architectury.fabriclike.ServerInterfaceFabricLike;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ServerInterfaceImpl {
    public static String getLoaderNameX() {
        return "Fabric";
    }
    public static String getLoaderVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getVersion().getFriendlyString() + " (MC: " + FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString()+ ")";
    }
    public static boolean playerHasPermissionsX(UUID player, String... permissions) {
        return ServerInterfaceFabricLike.playerHasPermissionsX(player,permissions);

    }
    public static boolean playerHasPermissionsX(Player player, String... permissions) {
        return ServerInterfaceFabricLike.playerHasPermissionsX(player,permissions);
    }
    public static boolean checkVanish(UUID player) {
        return ServerInterfaceFabricLike.checkVanish(player);
    }
}
