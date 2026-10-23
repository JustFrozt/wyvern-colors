package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.config.WyvernSettings;
import dev.justfrozt.wyverncolors.config.WyvernSettingsScreen;
import dev.justfrozt.wyverncolors.config.NativePotentialSettings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod("wyverncolors")
public final class ForgeMod {
    public ForgeMod(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) Client.initialize(context);
    }

    private static final class Client {
        static void initialize(FMLJavaModLoadingContext context) {
            WyvernSettings.initialize(FMLPaths.CONFIGDIR.get().resolve("wyverncolors.json"));
            NativePotentialSettings.initialize(FMLPaths.CONFIGDIR.get().resolve("wyverncolors-potential.json"),
                !LoaderPlatform.version("wynntils").equals("not installed"));
            context.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                    (minecraft, parent) -> new WyvernSettingsScreen(parent)));
            RenderDiagnostics.startup();
        }
    }
}
