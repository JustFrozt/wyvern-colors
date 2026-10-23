package dev.justfrozt.wyverncolors;

import net.neoforged.fml.ModList;

public final class LoaderPlatform {
    private LoaderPlatform() {}

    public static String version(String id) {
        return ModList.get().getModContainerById(id)
            .map(mod -> mod.getModInfo().getVersion().toString()).orElse("not installed");
    }
}
