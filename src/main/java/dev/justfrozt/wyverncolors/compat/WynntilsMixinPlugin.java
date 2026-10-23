package dev.justfrozt.wyverncolors.compat;

import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/** Optional integration checks actual hooks, rather than a version-number allowlist. */
public final class WynntilsMixinPlugin implements IMixinConfigPlugin {
    private static Boolean compatible;
    public static synchronized boolean supported() {
        if (compatible != null) return compatible;
        var loader = FabricLoader.getInstance();
        var mod = loader.getModContainer("wynntils");
        if (mod.isEmpty()) return compatible = false;
        var mappings = loader.getMappingResolver();
        var result = WynntilsHooks.inspect(name -> MixinService.getService().getBytecodeProvider()
                .getClassNode(name.replace('/', '.'), false),
            mappings.mapClassName("intermediary", "net.minecraft.class_332").replace('.', '/'),
            mappings.mapClassName("intermediary", "net.minecraft.class_1799").replace('.', '/'),
            mappings.mapClassName("intermediary", "net.minecraft.class_327").replace('.', '/'));
        var logger = org.slf4j.LoggerFactory.getLogger("wyverncolors");
        String version = mod.get().getMetadata().getVersion().getFriendlyString();
        if (result.supported()) logger.debug("[WyvernColors] Wynntils {} mount-text hooks verified", version);
        else logger.warn("[WyvernColors] Wynntils {} mount-text integration unavailable: {}. Reins and flute colors remain available.",
            version, result.reason());
        return compatible = result.supported();
    }
    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return supported(); }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String name, ClassNode target, String mixin, IMixinInfo info) {}
    @Override public void postApply(String name, ClassNode target, String mixin, IMixinInfo info) {
        if (!mixin.endsWith(".ItemTextOverlaySettingsMixin")) return;
        // Mixin erases signatures on added fields. Native Wynntils discovery requires
        // Config<Integer>/Config<Boolean>, rather than the erased Config class.
        int restored = 0;
        for (var field : target.fields) {
            String type = switch (field.name) {
                case "wyvernPotentialFontSize" -> "java/lang/Integer";
                case "wyvernPotentialAtBottom", "wyvernPotentialOverride" -> "java/lang/Boolean";
                case "wyvernPotentialPosition" -> "dev/justfrozt/wyverncolors/config/PotentialPosition";
                default -> null;
            };
            if (type == null) continue;
            if (!field.desc.equals("Lcom/wynntils/core/persisted/config/Config;")) {
                throw new IllegalStateException("Unexpected mount-potential setting type: " + field.name);
            }
            field.signature = "Lcom/wynntils/core/persisted/config/Config<L" + type + ";>;";
            restored++;
        }
        if (restored != 4) throw new IllegalStateException("Missing mount-potential settings fields");
    }
}
