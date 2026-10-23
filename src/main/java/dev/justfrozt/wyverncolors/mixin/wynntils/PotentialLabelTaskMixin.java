package dev.justfrozt.wyverncolors.mixin.wynntils;

import com.wynntils.utils.render.TextRenderTask;
import dev.justfrozt.wyverncolors.compat.PotentialLabelTask;
import dev.justfrozt.wyverncolors.config.MountLabelLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = TextRenderTask.class, remap = false)
public abstract class PotentialLabelTaskMixin implements PotentialLabelTask {
    @Unique private MountLabelLayout wyverncolors$layout;
    @Unique private boolean wyverncolors$potential;
    @Unique @Override public MountLabelLayout wyverncolors$getLayout() { return wyverncolors$layout; }
    @Unique @Override public void wyverncolors$setLayout(MountLabelLayout layout) { wyverncolors$layout = layout; }
    @Unique @Override public boolean wyverncolors$isPotential() { return wyverncolors$potential; }
    @Unique @Override public void wyverncolors$setPotential(boolean value) { wyverncolors$potential = value; }
}
