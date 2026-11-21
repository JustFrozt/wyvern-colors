package dev.justfrozt.wyverncolors;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import dev.justfrozt.wyverncolors.config.NativePotentialSettings;
import dev.justfrozt.wyverncolors.config.WyvernSettings;

public final class WyvernColorsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        var configDir = FabricLoader.getInstance().getConfigDir();
        WyvernSettings.initialize(configDir.resolve("wyverncolors.json"));
        NativePotentialSettings.initialize(configDir.resolve("wyverncolors-potential.json"));
        RenderDiagnostics.startup();
    }
}
