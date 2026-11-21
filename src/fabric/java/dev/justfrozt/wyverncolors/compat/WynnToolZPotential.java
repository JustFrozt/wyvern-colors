package dev.justfrozt.wyverncolors.compat;

import java.lang.reflect.Field;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

/** Reads WynnToolZ's two live toggles without loading any of its classes when absent. */
public final class WynnToolZPotential {
    private record Toggles(Object config, Field overlay, Field potential) {
        boolean active() throws IllegalAccessException {
            return overlay.getBoolean(config) && potential.getBoolean(config);
        }
    }

    private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("wtz");
    private static Toggles toggles;
    private static boolean initialized;

    private WynnToolZPotential() {}

    public static boolean active() {
        if (!INSTALLED) return false;
        if (!initialized) {
            initialized = true;
            try {
                Object config = Class.forName("xyz.mashtoolz.wtz.client.WTZClient").getField("CONFIG").get(null);
                Class<?> type = config.getClass();
                toggles = new Toggles(config, type.getField("mountItemOverlayEnabled"),
                    type.getField("mountItemOverlayPotentialEnabled"));
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                unavailable(failure);
            }
        }
        if (toggles == null) return false;
        try {
            return toggles.active();
        } catch (IllegalAccessException failure) {
            unavailable(failure);
            return false;
        }
    }

    private static void unavailable(Throwable failure) {
        toggles = null;
        LoggerFactory.getLogger("wyverncolors").warn("[WyvernColors] WynnToolZ potential settings unavailable", failure);
    }
}
