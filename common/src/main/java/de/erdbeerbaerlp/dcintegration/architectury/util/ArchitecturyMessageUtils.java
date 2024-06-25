package de.erdbeerbaerlp.dcintegration.architectury.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import de.erdbeerbaerlp.dcintegration.architectury.util.accessors.ShowInTooltipAccessor;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import de.erdbeerbaerlp.dcintegration.common.util.MessageUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.Arrays;

public class ArchitecturyMessageUtils extends MessageUtils {
    public static String formatPlayerName(ServerPlayer player) {
        if (player.getTabListDisplayName() != null)
            return ChatFormatting.stripFormatting(player.getTabListDisplayName().getString());
        else
            return ChatFormatting.stripFormatting(player.getName().getString());
    }

    public static MessageEmbed genItemStackEmbedIfAvailable(final Component component, Level w) {
        if (!Configuration.instance().forgeSpecific.sendItemInfo) return null;
        JsonObject json;
        try {
            final JsonElement jsonElement = JsonParser.parseString(Component.Serializer.toJson(component, w.registryAccess()));

            System.out.println(jsonElement);
            if (jsonElement.isJsonObject())
                json = jsonElement.getAsJsonObject();
            else return null;
        } catch (final IllegalStateException ex) {
            DiscordIntegration.LOGGER.error("There was an error parsing JSON", ex);
            return null;
        }
        System.out.println(json);
        if (json.has("with")) {
            final JsonArray args = json.getAsJsonArray("with");
            for (JsonElement el : args) {
                if (el instanceof JsonObject arg1) {
                    if (arg1.has("hoverEvent")) {
                        final JsonObject hoverEvent = arg1.getAsJsonObject("hoverEvent");
                        if (hoverEvent.has("action") && hoverEvent.get("action").getAsString().equals("show_item") && hoverEvent.has("contents")) {
                            if (hoverEvent.getAsJsonObject("contents").has("components")) {
                                final JsonObject item = hoverEvent.getAsJsonObject("contents").getAsJsonObject();
                                try {
                                    final CompoundTag tag = CompoundTagArgument.compoundTag().parse(new StringReader(item.toString()));
                                    final ItemStack is = ItemStack.parse(w.registryAccess(), tag).orElseThrow();

                                    final DataComponentMap itemTag = is.getComponents();
                                    final EmbedBuilder b = new EmbedBuilder();
                                    Component title = itemTag.getOrDefault(DataComponents.CUSTOM_NAME, Component.translatable(is.getItem().getDescriptionId(), is.getItem().getName(is).getString(), null));
                                    if (title.toString().isEmpty())
                                        title = Component.translatable(is.getItem().getDescriptionId());
                                    else
                                        b.setFooter(is.getItemHolder().getRegisteredName());
                                    b.setTitle(title.getString());
                                    final StringBuilder tooltip = new StringBuilder();
                                    //Add Enchantments
                                    if (itemTag.has(DataComponents.ENCHANTMENTS)) {
                                        final ItemEnchantments e = itemTag.get(DataComponents.ENCHANTMENTS);
                                        if (e != null)
                                            if (((ShowInTooltipAccessor) e).discordIntegration$showsInTooltip())
                                                for (Object2IntMap.Entry<Holder<Enchantment>> ench : e.entrySet()) {
                                                    tooltip.append(ChatFormatting.stripFormatting(ench.getKey().value().getFullname(ench.getKey(),e.getLevel(ench.getKey())).getString())).append("\n");
                                                }
                                    }
                                    //Add Lores
                                    if (itemTag.has(DataComponents.LORE)) {
                                        final ItemLore l = itemTag.get(DataComponents.LORE);
                                        if (l != null)
                                            for (Component line : l.lines()) {
                                                tooltip.append("_").append(line.getString()).append("_\n");
                                            }
                                    }
                                    //Add 'Unbreakable' Tag
                                    if (itemTag.has(DataComponents.UNBREAKABLE)) {
                                        final Unbreakable unb = itemTag.get(DataComponents.UNBREAKABLE);
                                        if (unb != null)
                                            if (unb.showInTooltip())
                                                tooltip.append("Unbreakable\n");
                                    }
                                    b.setDescription(tooltip.toString());
                                    return b.build();
                                } catch (CommandSyntaxException ignored) {
                                    DiscordIntegration.LOGGER.error("Error",ignored);
                                    //Just go on and ignore it
                                }
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
}
