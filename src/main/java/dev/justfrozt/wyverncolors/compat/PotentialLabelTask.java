package dev.justfrozt.wyverncolors.compat;

import dev.justfrozt.wyverncolors.config.MountLabelLayout;

/** A marker on the native task; other tasks keep a null layout. */
public interface PotentialLabelTask {
    MountLabelLayout wyverncolors$getLayout();
    void wyverncolors$setLayout(MountLabelLayout layout);
    boolean wyverncolors$isPotential();
    void wyverncolors$setPotential(boolean potential);
}
