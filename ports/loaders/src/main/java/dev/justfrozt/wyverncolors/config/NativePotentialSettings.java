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

/** Native-loader fallback. A compatible Wynntils backend keeps ownership of its settings. */
public final class NativePotentialSettings implements PotentialSettings.Backend {
    private static NativePotentialSettings current;
    private final Path file;
    private JsonObject document;
    private int size;
    private PotentialPosition position;
    private boolean override;
    private boolean saved = true;

    private NativePotentialSettings(Path file, JsonObject document, int size,
            PotentialPosition position, boolean override) {
        this.file = file.toAbsolutePath(); this.document = document;
        this.size = size; this.position = position; this.override = override;
    }

    public static void initialize(Path file, boolean wynntilsInstalled) {
        current = null;
        if (!wynntilsInstalled && PotentialSettings.backend() == null) {
            current = load(file);
            PotentialSettings.bind(current);
        }
    }

    public static NativePotentialSettings active() {
        return current != null && PotentialSettings.backend() == current ? current : null;
    }
    public static String statusKey() {
        return active() != null ? "screen.wyverncolors.potential.native"
            : "screen.wyverncolors.potential.shared";
    }
    public static String fontDescriptionKey() {
        return active() != null ? "config.wyverncolors.potentialFontSize.nativeDescription"
            : "config.wyverncolors.potentialFontSize.description";
    }
    public static boolean saveSucceeded() { return active() == null || current.saved; }

    public static NativePotentialSettings load(Path file) {
        JsonObject data = new JsonObject();
        int size = PotentialSettings.DEFAULT_SIZE;
        PotentialPosition position = PotentialSettings.DEFAULT_POSITION;
        boolean override = true;
        try {
            if (Files.isRegularFile(file)) {
                try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    data = JsonParser.parseReader(reader).getAsJsonObject();
                }
            }
            var font = data.get("fontSizePercent");
            if (font != null && font.isJsonPrimitive() && font.getAsJsonPrimitive().isNumber()) {
                try { size = PotentialSettings.clampSize(font.getAsBigDecimal().intValueExact()); }
                catch (ArithmeticException invalid) { size = PotentialSettings.DEFAULT_SIZE; }
            }
            var enabled = data.get("overrideEnabled");
            if (enabled != null && enabled.isJsonPrimitive() && enabled.getAsJsonPrimitive().isBoolean()) {
                override = enabled.getAsBoolean();
            }
            var corner = data.get("position");
            if (corner != null && corner.isJsonPrimitive() && corner.getAsJsonPrimitive().isString()) {
                try { position = PotentialPosition.valueOf(corner.getAsString()); }
                catch (IllegalArgumentException invalid) { position = PotentialSettings.DEFAULT_POSITION; }
            }
        } catch (IOException | RuntimeException invalid) {
            LoggerFactory.getLogger("wyverncolors").warn(
                "[WyvernColors] Could not read native potential-text settings; using defaults", invalid);
            data = new JsonObject(); size = PotentialSettings.DEFAULT_SIZE;
            position = PotentialSettings.DEFAULT_POSITION; override = true;
        }
        return new NativePotentialSettings(file, data, size, position, override);
    }

    @Override public int fontSizePercent() { return size; }
    @Override public PotentialPosition position() { return position; }
    @Override public boolean overrideEnabled() { return override; }
    @Override public boolean potentialSelected() { return true; }
    @Override public void setFontSizePercent(int value) { save(PotentialSettings.clampSize(value), position, override); }
    @Override public void setPosition(PotentialPosition value) { save(size, value == null ? PotentialSettings.DEFAULT_POSITION : value, override); }
    @Override public void setOverrideEnabled(boolean value) { save(size, position, value); }
    @Override public void migrateLegacyPosition() {}
    @Override public void resetDefaults() { save(PotentialSettings.DEFAULT_SIZE, PotentialSettings.DEFAULT_POSITION, true); }

    private void save(int nextSize, PotentialPosition nextPosition, boolean nextOverride) {
        JsonObject next = document.deepCopy();
        next.addProperty("fontSizePercent", nextSize);
        next.addProperty("position", nextPosition.name());
        next.addProperty("overrideEnabled", nextOverride);
        Path temporary = null;
        try {
            Files.createDirectories(file.getParent());
            temporary = Files.createTempFile(file.getParent(), "wyverncolors-potential-", ".tmp");
            Files.writeString(temporary, new GsonBuilder().setPrettyPrinting().create().toJson(next)
                + "\n", StandardCharsets.UTF_8);
            try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
            document = next; size = nextSize; position = nextPosition; override = nextOverride; saved = true;
        } catch (IOException failure) {
            saved = false;
            LoggerFactory.getLogger("wyverncolors").error(
                "[WyvernColors] Could not save native potential-text settings", failure);
        } finally {
            if (temporary != null) {
                try { Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }
}
