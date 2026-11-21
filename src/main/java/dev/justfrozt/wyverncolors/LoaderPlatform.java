package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.compat.WynnToolZPotential;
import dev.justfrozt.wyverncolors.config.PotentialSettings.Source;
import dev.justfrozt.wyverncolors.config.WyvernSettings;
import net.fabricmc.loader.api.FabricLoader;

/** Loader metadata used by the optional troubleshooting log. */
public final class LoaderPlatform {
    private LoaderPlatform() {}

    public static Source potentialSource() {
        if (WynnToolZPotential.active()) return Source.WYNNTOOLZ;
        if (WyvernSettings.current().useWynntilsPotential() && wynntilsPotentialAvailable()) {
            return Source.WYNNTILS;
        }
        return Source.WYVERN_COLORS;
    }

    public static boolean wynntilsPotentialAvailable() {
        return FabricLoader.getInstance().isModLoaded("wynntils");
    }

    public static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("not installed");
    }
}
