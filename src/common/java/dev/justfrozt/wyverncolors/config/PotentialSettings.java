package dev.justfrozt.wyverncolors.config;

/** Layout settings and the available potential display sources. */
public final class PotentialSettings {
    public enum Source { WYVERN_COLORS, WYNNTOOLZ, WYNNTILS }

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
            "[WyvernColors] Potential-text settings attached");
    }
    public static Backend backend() { return backend; }
    public static boolean available() { return backend != null; }
    public static int clampSize(Integer value) {
        return value == null ? DEFAULT_SIZE : Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
    }
}
