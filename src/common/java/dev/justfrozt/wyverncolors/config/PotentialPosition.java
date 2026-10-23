package dev.justfrozt.wyverncolors.config;

import java.util.Locale;

public enum PotentialPosition {
    BOTTOM(.5f, true), BOTTOM_LEFT(0, true), BOTTOM_RIGHT(1, true),
    TOP(.5f, false), TOP_LEFT(0, false), TOP_RIGHT(1, false);

    private final float horizontalAnchor;
    private final boolean bottom;
    PotentialPosition(float horizontalAnchor, boolean bottom) {
        this.horizontalAnchor = horizontalAnchor;
        this.bottom = bottom;
    }
    public float horizontalAnchor() { return horizontalAnchor; }
    public boolean bottom() { return bottom; }
    public PotentialPosition next() { return values()[(ordinal() + 1) % values().length]; }
    public String translationKey() { return "screen.wyverncolors.potential." + name().toLowerCase(Locale.ROOT); }
}
