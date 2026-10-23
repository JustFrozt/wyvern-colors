package dev.justfrozt.wyverncolors;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/** Appearance keys from the server's separate lore line, never from the mount name. */
public record Appearance(Scale scale, Horn horn) {
    public enum Scale {
        AZURE(0x25AFC7), CERULEAN(0x2911C8), BRONZE(0x8CC747), EBONY(0x292930),
        FLEDGE(0xCD95C2), GOLDEN(0xE6C413), HOLLOW(0x7A868B), INFERNAL(0xD3350F),
        JADE(0x429341), MYSTIC(0x7D1FB9);

        public final int rgb;
        Scale(int rgb) { this.rgb = rgb; }
    }

    public enum Horn {
        CINDER(0x4D6651), HORN(0xB28D54), KANDER(0x5E5E5E), ONYX(0x0D0D0D),
        QUARTZ(0xE6E5E4), SAPPHIRE(0x92DDE5), ROSE(0xE36C6E), SHELL(0x27ACC1),
        IVORY(0xD2A9DA), TUSK(0x744C32);

        public final int rgb;
        Horn(int rgb) { this.rgb = rgb; }
    }

    public boolean isMystic() { return scale == Scale.MYSTIC; }

    // Bright and midtone swirl pixels follow the original texture's luminance.
    // The four adjoining shadow pixels blend 20% toward the original leather.
    private static final double SWIRL_MIDTONE = luminance(0x8B4B18) / luminance(0xA95D30);
    private static final double SWIRL_SHADOW = luminance(0x663004) / luminance(0xA95D30);

    /** Shared tints keep the original leather brown around the selected scale mark. */
    public List<Integer> modelTints() {
        return List.of(
            0xFFFFFF,
            lighten(horn.rgb, 0.20), horn.rgb, darken(horn.rgb, 0.94),
            darken(horn.rgb, 0.80), darken(horn.rgb, 0.72), darken(horn.rgb, 0.68),
            scale.rgb, darken(scale.rgb, SWIRL_MIDTONE),
            0x555555, 0x505050, 0x494949,
            0x663004, 0x2F2F2F, 0x45210D, 0x180505,
            0xFFEEF8, lighten(scale.rgb, 0.62), lighten(scale.rgb, 0.30),
            0xA95D30, leatherShadow(scale.rgb)
        );
    }

    /** The alternate model has independent tint slots for the swapped regions. */
    public List<Integer> modelTints(boolean swapColors) {
        if (!swapColors) return modelTints();
        List<Integer> tints = new ArrayList<>(modelTints());
        tints.addAll(List.of(
            horn.rgb, darken(horn.rgb, SWIRL_MIDTONE), leatherShadow(horn.rgb),
            lighten(scale.rgb, 0.20), scale.rgb, darken(scale.rgb, 0.94),
            darken(scale.rgb, 0.80), darken(scale.rgb, 0.72), darken(scale.rgb, 0.68)
        ));
        // Sparkle slots 16..18 remain derived from scales, including Mystic.
        return List.copyOf(tints);
    }

    /** Bright flat horn dots and a shadowed horn-colored recess on the shaded scale body. */
    public List<Integer> fluteTints() {
        int hornValue = maxChannel(horn.rgb);
        int dot = fluteShade(horn.rgb, (hornValue + (255 - hornValue) * 0.55) / hornValue);
        // The recess stays below the surrounding body's darkest shade, even
        // for pale horns on dark scales. Its lower-right corner catches a little light.
        double holeValue = Math.min(hornValue * 0.36, maxChannel(scale.rgb) * 0.28);
        int hole = fluteShade(horn.rgb, holeValue / hornValue);
        double cornerValue = Math.min(Math.max(holeValue * 1.18, holeValue + 3),
                                      maxChannel(scale.rgb) * 0.34);
        int corner = fluteShade(horn.rgb, cornerValue / hornValue);
        return List.of(
            0xFFFFFF,
            scale.rgb, fluteShade(scale.rgb, 193.0 / 255.0), fluteShade(scale.rgb, 140.0 / 255.0),
            dot, hole, dot, corner,
            0x505050, 0x180505
        );
    }

    private static int fluteShade(int rgb, double factor) {
        // Scaling RGB by value preserves the hue and saturation of each horn swatch.
        return pack(Math.min(255, (int) Math.round(((rgb >> 16) & 255) * factor)),
                    Math.min(255, (int) Math.round(((rgb >> 8) & 255) * factor)),
                    Math.min(255, (int) Math.round((rgb & 255) * factor)));
    }

    private static int maxChannel(int rgb) {
        return Math.max((rgb >> 16) & 255, Math.max((rgb >> 8) & 255, rgb & 255));
    }

    private static double luminance(int rgb) {
        return ((rgb >> 16) & 255) * 0.2126 + ((rgb >> 8) & 255) * 0.7152
            + (rgb & 255) * 0.0722;
    }

    private static int leatherShadow(int rgb) {
        return pack((int) Math.round(((rgb >> 16) & 255) * SWIRL_SHADOW * 0.80 + 102 * 0.20),
                    (int) Math.round(((rgb >> 8) & 255) * SWIRL_SHADOW * 0.80 + 48 * 0.20),
                    (int) Math.round((rgb & 255) * SWIRL_SHADOW * 0.80 + 4 * 0.20));
    }

    private static int darken(int rgb, double factor) {
        return pack((int) Math.round(((rgb >> 16) & 255) * factor),
                    (int) Math.round(((rgb >> 8) & 255) * factor),
                    (int) Math.round((rgb & 255) * factor));
    }

    private static int lighten(int rgb, double amount) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        return pack((int) Math.round(r + (255 - r) * amount),
                    (int) Math.round(g + (255 - g) * amount),
                    (int) Math.round(b + (255 - b) * amount));
    }

    private static int pack(int r, int g, int b) { return (r << 16) | (g << 8) | b; }

    public String key() {
        return scale.name().toLowerCase(Locale.ROOT) + "-" + horn.name().toLowerCase(Locale.ROOT);
    }
}
