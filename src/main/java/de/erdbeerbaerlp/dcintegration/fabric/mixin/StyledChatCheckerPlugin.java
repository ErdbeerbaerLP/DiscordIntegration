package de.erdbeerbaerlp.dcintegration.fabric.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * This plugin checks if StyledChat is installed.
 * If it is, we skip applying the StyledChatMixin so there's no conflict.
 */
public class StyledChatCheckerPlugin implements IMixinConfigPlugin {
    private boolean shouldApplyStyledChatMixin = false;

    @Override
    public void onLoad(String mixinPackage) {
        boolean styledChatInstalled = FabricLoader.getInstance().isModLoaded("styledchat");
        shouldApplyStyledChatMixin = !styledChatInstalled;
    }

    @Override
    public List<String> getMixins() {
        if (shouldApplyStyledChatMixin) {
            return List.of("StyledChatMixin");
        } else {
            return List.of();
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }
}

