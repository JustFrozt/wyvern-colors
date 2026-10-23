package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.config.WyvernSettings;
import dev.justfrozt.wyverncolors.config.WyvernSettingsScreen;
import dev.justfrozt.wyverncolors.config.NativePotentialSettings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = "wyverncolors", dist = Dist.CLIENT)
public final class NeoForgeClient {
    public NeoForgeClient(ModContainer container) {
        WyvernSettings.initialize(FMLPaths.CONFIGDIR.get().resolve("wyverncolors.json"));
        NativePotentialSettings.initialize(FMLPaths.CONFIGDIR.get().resolve("wyverncolors-potential.json"),
            !LoaderPlatform.version("wynntils").equals("not installed"));
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (mod, parent) -> new WyvernSettingsScreen(parent));
        RenderDiagnostics.startup();
    }
}
