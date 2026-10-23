package dev.justfrozt.wyverncolors;

import net.fabricmc.loader.api.FabricLoader;

/** Loader metadata used by the optional troubleshooting log. */
public final class LoaderPlatform {
    private LoaderPlatform() {}

    public static String version(String id) {
        return FabricLoader.getInstance().getModContainer(id)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString()).orElse("not installed");
    }
}
