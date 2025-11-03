package de.erdbeerbaerlp.dcintegration.architectury.fabriclike;

import de.erdbeerbaerlp.dcintegration.common.util.MinecraftPermission;
import me.drex.vanish.api.VanishAPI;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod.server;

public class ServerInterfaceFabricLike {
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
    public static boolean checkVanish(UUID player){
        if(FabricLoaderImpl.INSTANCE.isModLoaded("melius-vanish")){
            if(VanishAPI.isVanished(server, player)) return true;
        }
        return false;
    }
}
