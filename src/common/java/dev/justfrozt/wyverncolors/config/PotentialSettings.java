package dev.justfrozt.wyverncolors.config;

/** Both settings screens and every draw read the same Wynntils-owned values. */
public final class PotentialSettings {
    public static final int DEFAULT_SIZE = 70;
    public static final PotentialPosition DEFAULT_POSITION = PotentialPosition.TOP_LEFT;
    public static final int MIN_SIZE = 50;
    public static final int MAX_SIZE = 125;

    public interface Backend {
        int fontSizePercent();
        PotentialPosition position();
        boolean overrideEnabled();
        boolean potentialSelected();
        void setFontSizePercent(int value);
        void setPosition(PotentialPosition value);
        void migrateLegacyPosition();
        void setOverrideEnabled(boolean value);
        void resetDefaults();
    }

    private static Backend backend;

    private PotentialSettings() {}

    public static void bind(Backend value) {
        backend = value;
        org.slf4j.LoggerFactory.getLogger("wyverncolors").debug(
            "[WyvernColors] Shared Wynntils potential-text settings attached");
    }
    public static Backend backend() { return backend; }
    public static boolean available() { return backend != null; }
    public static int clampSize(Integer value) {
        return value == null ? DEFAULT_SIZE : Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
    }
}
