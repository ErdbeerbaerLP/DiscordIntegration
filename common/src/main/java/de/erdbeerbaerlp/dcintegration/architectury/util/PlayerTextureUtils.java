package de.erdbeerbaerlp.dcintegration.architectury.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.INSTANCE;
import static de.erdbeerbaerlp.dcintegration.common.DiscordIntegration.LOGGER;

public class PlayerTextureUtils {
    static public String getPlayerTextureHash(Player player) {
        try {
            String textureData = player.getGameProfile().properties().get("textures").iterator().next().value();
            textureData = new String(Base64.getDecoder().decode(textureData), StandardCharsets.UTF_8);
            String url = (new Gson()).fromJson(textureData, JsonObject.class).getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            // remove "http://textures.minecraft.net/texture/" but generically
            return url.replaceAll(".*\\/", "");
        }
        catch (Exception e) {
            return "null";
        }
    }

    static public String getAvatarUrl(Player player) {
        return INSTANCE.getSkinURL()
                .replace("%uuid%", player.getUUID().toString())
                .replace("%uuid_dashless%", player.getUUID().toString().replace("-", ""))
                .replace("%name%", player.getName().getString())
                .replace("%texture_hash%", getPlayerTextureHash(player))
                .replace("%randomUUID%", UUID.randomUUID().toString());
    }
}
