package dev.justfrozt.wyverncolors.compat;

import com.wynntils.utils.render.TextRenderTask;
import dev.justfrozt.wyverncolors.config.PotentialOpacity;

/** Copy the style so cached Wynntils tasks never accumulate opacity changes. */
public final class PotentialTextOpacity {
    private PotentialTextOpacity() {}
    public static TextRenderTask apply(TextRenderTask task, int percent) {
        if (PotentialOpacity.clamp(percent) == 100) return task;
        var style = task.getSetting();
        var color = style.customColor();
        int alpha = PotentialOpacity.apply(color.asInt(), percent) >>> 24;
        return new TextRenderTask(task.getText(), style.withCustomColor(color.withAlpha(alpha)));
    }
}
