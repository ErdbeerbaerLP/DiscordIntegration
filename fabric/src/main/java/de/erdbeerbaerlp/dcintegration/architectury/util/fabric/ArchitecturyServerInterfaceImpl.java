package de.erdbeerbaerlp.dcintegration.architectury.util.fabric;

import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class ArchitecturyServerInterfaceImpl {
    public static String getLoaderNameX() {
        return "Fabric";
    }
    public static String getLoaderVersion() {
        return FabricLoader.getInstance().getModContainer("fabricloader").get().getMetadata().getVersion().getFriendlyString() + " (MC: " + FabricLoader.getInstance().getModContainer("minecraft").get().getMetadata().getVersion().getFriendlyString()+ ")";
    }
    public static boolean playerHasPermissionsX(UUID player, String... permissions) {
        for (String permission : permissions) {
            for (final MinecraftPermission perm : MinecraftPermission.values()) {
                if(perm.getAsString().equals(permission)){
                    if(Permissions.check(player,perm.getAsString(), perm.getDefaultValue()).join()){
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static boolean playerHasPermissionsX(Player player, String... permissions) {
        for (String permission : permissions) {
            for (MinecraftPermission value : MinecraftPermission.values()) {
                if(value.getAsString().equals(permission)){
                    if(Permissions.check(player,value.getAsString(), value.getDefaultValue())){
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
