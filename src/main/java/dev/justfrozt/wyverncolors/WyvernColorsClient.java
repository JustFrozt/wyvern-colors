package dev.justfrozt.wyverncolors;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import dev.justfrozt.wyverncolors.config.WyvernSettings;

public final class WyvernColorsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        WyvernSettings.initialize(FabricLoader.getInstance().getConfigDir().resolve("wyverncolors.json"));
        RenderDiagnostics.startup();
    }
}
