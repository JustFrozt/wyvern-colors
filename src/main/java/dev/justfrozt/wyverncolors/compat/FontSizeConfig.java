package dev.justfrozt.wyverncolors.compat;

import com.wynntils.core.persisted.config.Config;
import dev.justfrozt.wyverncolors.config.PotentialSettings;

/** Native editor, config reloads, and the settings slider all use the same accepted range. */
public final class FontSizeConfig extends Config<Integer> {
    public FontSizeConfig() { super(PotentialSettings.DEFAULT_SIZE); }
    @Override public void setValue(Integer value) { super.setValue(PotentialSettings.clampSize(value)); }
    @Override public void store(Integer value) { super.store(PotentialSettings.clampSize(value)); }
}
