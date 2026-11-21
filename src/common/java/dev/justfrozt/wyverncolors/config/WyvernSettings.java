package dev.justfrozt.wyverncolors.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.LoggerFactory;

/** Local client preferences. */
public final class WyvernSettings {
    public static final boolean DEFAULT_SWAP_COLORS = true;
    public static final boolean DEFAULT_DEBUG_LOGGING = false;
    private static final String SWAP_KEY = "swapScaleAndHornColors";
    private static final String DEBUG_KEY = "debugLogging";
    private static final String OPACITY_KEY = "potentialOpacityPercent";
    private static final String WYNNTILS_POTENTIAL_KEY = "useWynntilsPotential";
    private static WyvernSettings current = new WyvernSettings(null, new JsonObject(),
        DEFAULT_SWAP_COLORS, DEFAULT_DEBUG_LOGGING, PotentialOpacity.DEFAULT, false);
    private final Path file;
    private JsonObject document;
    private boolean swapColors;
    private boolean debugLogging;
    private int potentialOpacityPercent;
    private boolean useWynntilsPotential;

    private WyvernSettings(Path file, JsonObject document, boolean swapColors, boolean debugLogging,
            int opacity, boolean useWynntilsPotential) {
        this.file = file;
        this.document = document;
        this.swapColors = swapColors;
        this.debugLogging = debugLogging;
        this.potentialOpacityPercent = opacity;
        this.useWynntilsPotential = useWynntilsPotential;
    }

    public static void initialize(Path file) { current = load(file); }
    public static WyvernSettings current() { return current; }
    public boolean swapColors() { return swapColors; }
    public boolean debugLogging() { return debugLogging; }
    public int potentialOpacityPercent() { return potentialOpacityPercent; }
    public boolean useWynntilsPotential() { return useWynntilsPotential; }

    public static WyvernSettings load(Path file) {
        JsonObject document = new JsonObject();
        boolean swapColors = DEFAULT_SWAP_COLORS;
        boolean debugLogging = DEFAULT_DEBUG_LOGGING;
        int opacity = PotentialOpacity.DEFAULT;
        boolean useWynntilsPotential = false;
        if (Files.isRegularFile(file)) {
            try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                document = JsonParser.parseReader(reader).getAsJsonObject();
                swapColors = booleanValue(document, SWAP_KEY, DEFAULT_SWAP_COLORS);
                debugLogging = booleanValue(document, DEBUG_KEY, DEFAULT_DEBUG_LOGGING);
                useWynntilsPotential = booleanValue(document, WYNNTILS_POTENTIAL_KEY, false);
                var value = document.get(OPACITY_KEY);
                if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    try { opacity = PotentialOpacity.clamp(value.getAsBigDecimal().intValueExact()); }
                    catch (ArithmeticException invalid) { opacity = PotentialOpacity.DEFAULT; }
                }
            } catch (IOException | RuntimeException error) {
                LoggerFactory.getLogger("wyverncolors").warn(
                    "[WyvernColors] Could not read client settings; using defaults", error);
                document = new JsonObject();
                swapColors = DEFAULT_SWAP_COLORS;
                debugLogging = DEFAULT_DEBUG_LOGGING;
                opacity = PotentialOpacity.DEFAULT;
                useWynntilsPotential = false;
            }
        }
        return new WyvernSettings(file.toAbsolutePath(), document, swapColors, debugLogging, opacity, useWynntilsPotential);
    }

    public boolean setSwapColors(boolean value) {
        return save(value, debugLogging, potentialOpacityPercent, useWynntilsPotential);
    }

    public boolean setDebugLogging(boolean value) {
        return save(swapColors, value, potentialOpacityPercent, useWynntilsPotential);
    }

    public boolean setPotentialOpacityPercent(int value) {
        return save(swapColors, debugLogging, PotentialOpacity.clamp(value), useWynntilsPotential);
    }

    public boolean setUseWynntilsPotential(boolean value) {
        return save(swapColors, debugLogging, potentialOpacityPercent, value);
    }

    public boolean resetDefaults() {
        return save(DEFAULT_SWAP_COLORS, DEFAULT_DEBUG_LOGGING, PotentialOpacity.DEFAULT, false);
    }

    private static boolean booleanValue(JsonObject document, String key, boolean fallback) {
        var value = document.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()
            ? value.getAsBoolean() : fallback;
    }

    private boolean save(boolean nextSwapColors, boolean nextDebugLogging, int nextOpacity, boolean nextUseWynntils) {
        if (nextSwapColors == swapColors && nextDebugLogging == debugLogging
                && nextOpacity == potentialOpacityPercent && nextUseWynntils == useWynntilsPotential) return true;
        JsonObject updated = document.deepCopy();
        updated.addProperty(SWAP_KEY, nextSwapColors);
        updated.addProperty(DEBUG_KEY, nextDebugLogging);
        updated.addProperty(OPACITY_KEY, nextOpacity);
        updated.addProperty(WYNNTILS_POTENTIAL_KEY, nextUseWynntils);
        Path temporary = null;
        try {
            if (file != null) {
                Files.createDirectories(file.getParent());
                temporary = Files.createTempFile(file.getParent(), "wyverncolors-", ".tmp");
                Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create()
                    .toJson(updated) + "\n", StandardCharsets.UTF_8);
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException unsupported) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            document = updated;
            swapColors = nextSwapColors;
            debugLogging = nextDebugLogging;
            potentialOpacityPercent = nextOpacity;
            useWynntilsPotential = nextUseWynntils;
            return true;
        } catch (IOException error) {
            LoggerFactory.getLogger("wyverncolors").error("[WyvernColors] Could not save client settings", error);
            return false;
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
            }
        }
    }
}
