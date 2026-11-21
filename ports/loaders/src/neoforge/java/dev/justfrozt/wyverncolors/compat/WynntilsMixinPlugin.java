package dev.justfrozt.wyverncolors.compat;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional integration checks actual hooks, rather than a version-number allowlist. */
public final class WynntilsMixinPlugin implements IMixinConfigPlugin {
    private static Boolean compatible;
    public static synchronized boolean supported() {
        if (compatible != null) return compatible;
        compatible = WynntilsHooks.hasPotentialVisibilityHook(name -> MixinService.getService()
            .getBytecodeProvider().getClassNode(name.replace('/', '.'), false));
        return compatible;
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return supported(); }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode target, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode target, String mixin, IMixinInfo info) {}
}
