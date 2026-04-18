package de.erdbeerbaerlp.dcintegration.fabric;

import de.erdbeerbaerlp.dcintegration.DiscordIntegrationMod;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.common.storage.Configuration;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class DiscordIntegrationFabric implements DedicatedServerModInitializer {
    @Override
    public void onInitializeServer() {
        DiscordIntegrationMod.init();
        if ("INSERT BOT TOKEN HERE".equals(Configuration.instance().general.botToken)) {
            DiscordIntegration.LOGGER.error("Please check the config file and set an bot token");
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(DiscordIntegrationMod::serverStarted);
        ServerLifecycleEvents.SERVER_STARTING.register(DiscordIntegrationMod::serverStarting);
        ServerLifecycleEvents.SERVER_STARTING.register(this::serverStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(DiscordIntegrationMod::serverStopped);
        ServerLifecycleEvents.SERVER_STOPPING.register(DiscordIntegrationMod::serverStopping);
    }

    private void serverStarting(net.minecraft.server.MinecraftServer minecraftServer) {
        if (FabricLoader.getInstance().isModLoaded("dynmap")) {
            registerDynmapHook();
        }
        registerVanishHook();
    }

    // Dynmap API classes are optional, so register through reflection.
    private void registerDynmapHook() {
        try {
            final Object dynmapListener = Class.forName("de.erdbeerbaerlp.dcintegration.common.compat.DynmapListener")
                    .getDeclaredConstructor()
                    .newInstance();
            dynmapListener.getClass().getMethod("register").invoke(dynmapListener);
        } catch (Throwable ignored) {
        }
    }

    private void registerVanishHook() {
        if (!FabricLoader.getInstance().isModLoaded("melius-vanish")) {
            return;
        }

        try {
            Class<?> vanishEvents = Class.forName("me.drex.vanish.api.VanishEvents");
            Object vanishEvent = vanishEvents.getField("VANISH_EVENT").get(null);
            Method registerMethod = null;
            for (Method method : vanishEvent.getClass().getMethods()) {
                if ("register".equals(method.getName()) && method.getParameterCount() == 1) {
                    registerMethod = method;
                    break;
                }
            }
            if (registerMethod == null) {
                return;
            }

            Class<?> listenerType = registerMethod.getParameterTypes()[0];
            Object listener = Proxy.newProxyInstance(
                    DiscordIntegrationFabric.class.getClassLoader(),
                    new Class[]{listenerType},
                    (proxy, method, args) -> {
                        if (args != null && args.length == 2 && args[0] instanceof ServerPlayer player && args[1] instanceof Boolean vanished) {
                            DiscordIntegrationMod.vanish(player, vanished);
                        }
                        return null;
                    }
            );
            registerMethod.invoke(vanishEvent, listener);
        } catch (Throwable ignored) {
        }
    }
}
