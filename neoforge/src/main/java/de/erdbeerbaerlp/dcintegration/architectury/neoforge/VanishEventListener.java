package de.erdbeerbaerlp.dcintegration.architectury.neoforge;

import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import redstonedubstep.mods.vanishmod.api.PlayerVanishEvent;


public class VanishEventListener {
    @SubscribeEvent
    public void vanishEvent(PlayerVanishEvent ev){
        DiscordIntegrationMod.vanish((ServerPlayer) ev.getEntity(), ev.isVanished());
    }
}
