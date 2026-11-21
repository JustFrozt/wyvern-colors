package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.config.PotentialSettings.Source;
import dev.justfrozt.wyverncolors.config.WyvernSettings;
import net.neoforged.fml.ModList;

public final class LoaderPlatform {
    private LoaderPlatform() {}

    public static Source potentialSource() {
        return WyvernSettings.current().useWynntilsPotential() && wynntilsPotentialAvailable()
            ? Source.WYNNTILS : Source.WYVERN_COLORS;
    }

    public static boolean wynntilsPotentialAvailable() {
        return ModList.get().getModContainerById("wynntils").isPresent();
    }

    public static String version(String id) {
        return ModList.get().getModContainerById(id)
            .map(mod -> mod.getModInfo().getVersion().toString()).orElse("not installed");
    }
}
