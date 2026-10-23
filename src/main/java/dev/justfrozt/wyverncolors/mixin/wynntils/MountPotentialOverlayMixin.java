package dev.justfrozt.wyverncolors.mixin.wynntils;

import com.wynntils.features.inventory.ItemTextOverlayFeature;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderTask;
import dev.justfrozt.wyverncolors.compat.PotentialLabelTask;
import dev.justfrozt.wyverncolors.compat.PotentialTextBounds;
import dev.justfrozt.wyverncolors.config.MountLabelLayout;
import dev.justfrozt.wyverncolors.config.PotentialSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "com.wynntils.features.inventory.ItemTextOverlayFeature$MountItemOverlay", remap = false)
public abstract class MountPotentialOverlayMixin {
    @Shadow @Final private ItemTextOverlayFeature this$0;

    @ModifyArgs(method = "getTextOverlay", at = @At(value = "INVOKE",
        target = "Lcom/wynntils/features/inventory/ItemTextOverlayFeature$TextOverlay;<init>(Lcom/wynntils/utils/render/TextRenderTask;IIF)V"))
    private void wyverncolors$layoutPotential(Args args) {
        TextRenderTask task = args.get(0);
        ((PotentialLabelTask) (Object) task).wyverncolors$setLayout(null);
        PotentialSettings.Backend settings = (PotentialSettings.Backend) this$0;
        ((PotentialLabelTask) (Object) task).wyverncolors$setPotential(settings.potentialSelected());
        if (!settings.overrideEnabled() || !settings.potentialSelected()) return;
        var font = FontRenderer.getInstance().getFont();
        var bounds = PotentialTextBounds.measure(font, task);
        if (bounds == null) return;
        MountLabelLayout layout = MountLabelLayout.potential(bounds, task.getText().getComponent().getString(),
            settings.fontSizePercent(), settings.position());
        ((PotentialLabelTask) (Object) task).wyverncolors$setLayout(layout);
        args.set(1, 0);
        args.set(2, 0);
        args.set(3, layout.scale());
        dev.justfrozt.wyverncolors.RenderDiagnostics.potentialLayoutSelected(
            settings.fontSizePercent(), settings.position(), layout);
    }
}
