package dev.justfrozt.wyverncolors;

import net.minecraftforge.fml.ModList;

public final class LoaderPlatform {
    private LoaderPlatform() {}

    public static dev.justfrozt.wyverncolors.config.PotentialSettings.Source potentialSource() { return null; }
    public static boolean wynntilsPotentialAvailable() { return false; }

    public static String version(String id) {
        return ModList.get().getModContainerById(id)
            .map(mod -> mod.getModInfo().getVersion().toString()).orElse("not installed");
    }
}
