package de.erdbeerbaerlp.dcintegration.architectury.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;

public class SerializeComponentUtils {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    /**
     * Serializes a Component to a JSON string using the given registry access.
     */
    public static String toJson(Component component, RegistryAccess registryAccess) {
        JsonElement jsonElement = ComponentSerialization.CODEC
                .encodeStart(registryAccess.createSerializationContext(JsonOps.INSTANCE), component)
                .getOrThrow(JsonParseException::new);

        return GSON.toJson(jsonElement);
    }

    /**
     * Serializes a Component to a JSON string using the given holder lookup.
     */
    public static String toJson(Component component, HolderLookup.Provider holderLookup) {
        JsonElement jsonElement = ComponentSerialization.CODEC
                .encodeStart(holderLookup.createSerializationContext(JsonOps.INSTANCE), component)
                .getOrThrow(JsonParseException::new);

        return GSON.toJson(jsonElement);
    }

    /**
     * Deserializes a JSON string into a MutableComponent using the given registry access.
     */
    public static MutableComponent fromJson(String json, RegistryAccess registryAccess) {
        JsonElement jsonElement = JsonParser.parseString(json);
        Component component = ComponentSerialization.CODEC
                .parse(registryAccess.createSerializationContext(JsonOps.INSTANCE), jsonElement)
                .getOrThrow(JsonParseException::new);

        // Convert Component to MutableComponent
        return component.copy();
    }

    /**
     * Deserializes a JSON string into a MutableComponent using the given holder lookup.
     */
    public static MutableComponent fromJson(String json, HolderLookup.Provider holderLookup) {
        JsonElement jsonElement = JsonParser.parseString(json);
        Component component = ComponentSerialization.CODEC
                .parse(holderLookup.createSerializationContext(JsonOps.INSTANCE), jsonElement)
                .getOrThrow(JsonParseException::new);

        // Convert Component to MutableComponent
        return component.copy();
    }
}