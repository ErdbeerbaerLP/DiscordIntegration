package de.erdbeerbaerlp.dcintegration.architectury.utils.quilt;

import de.erdbeerbaerlp.dcintegration.architectury.fabriclike.ServerInterfaceFabricLike;
import net.minecraft.world.entity.player.Player;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.UUID;

public class ServerInterfaceImpl {
    public static String getLoaderNameX() {
        return "Quilt";
    }
    public static String getLoaderVersion() {
        return QuiltLoader.getModContainer("quilt_loader").get().metadata().version().raw() + " (MC: " + QuiltLoader.getModContainer("minecraft").get().metadata().version().raw()+ ")";
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
