package dev.justfrozt.wyverncolors.compat;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

public final class PotentialCompatPlugin implements IMixinConfigPlugin {
    private static Boolean wynntilsCompatible;

    @Override public boolean shouldApplyMixin(String target, String mixin) {
        if (mixin.endsWith(".WynnExtrasRenderContextMixin") || mixin.endsWith(".WynnExtrasMountColorMixin")) {
            return FabricLoader.getInstance().getModContainer("wynnextras")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("0.19.0"))
                .orElse(false);
        }
        if (mixin.endsWith(".WynnToolZPotentialMixin")) {
            // The optional WynnToolZ hooks depend on the released renderer's local variables.
            return FabricLoader.getInstance().getModContainer("wtz")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString().equals("2.3.1"))
                .orElse(false);
        }
        if (wynntilsCompatible == null) wynntilsCompatible = hasWynntilsPotentialHook();
        return wynntilsCompatible;
    }

    private static boolean hasWynntilsPotentialHook() {
        if (!FabricLoader.getInstance().isModLoaded("wynntils")) return false;
        return WynntilsHooks.hasPotentialVisibilityHook(name -> MixinService.getService()
            .getBytecodeProvider().getClassNode(name.replace('/', '.'), false));
    }

    @Override public void onLoad(String packageName) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> ownTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode target, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode target, String mixin, IMixinInfo info) {}
}
