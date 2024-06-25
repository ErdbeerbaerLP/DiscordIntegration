package de.erdbeerbaerlp.dcintegration.architectury.mixin;

import de.erdbeerbaerlp.dcintegration.architectury.metrics.Metrics;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod.bstats;

@Mixin(MinecraftServer.class)
public class MixinMinecraftServer {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo info) {

        Metrics.capturedServer.set((MinecraftServer) (Object) this);
        bstats = new Metrics(9765);
        bstats.addCustomChart(new Metrics.SimplePie("webhook_mode", () -> Configuration.instance().webhook.enable ? "Enabled" : "Disabled"));
        bstats.addCustomChart(new Metrics.SimplePie("command_log", () -> !Configuration.instance().commandLog.channelID.equals("0") ? "Enabled" : "Disabled"));
        bstats.addCustomChart(new Metrics.SimplePie("loader",()->Metrics.capturedServer.get().getServerModName()));

    }

    @Inject(method = "stopServer", at = @At("HEAD"))
    private void onShutdown(CallbackInfo info) {
        Metrics.capturedServer.compareAndSet((MinecraftServer) (Object) this, null);
    }

}