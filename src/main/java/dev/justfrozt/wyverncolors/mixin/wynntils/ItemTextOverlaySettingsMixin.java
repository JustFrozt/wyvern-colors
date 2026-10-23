package dev.justfrozt.wyverncolors.mixin.wynntils;

import com.wynntils.core.persisted.Persisted;
import com.wynntils.core.persisted.config.Config;
import com.wynntils.core.persisted.config.HiddenConfig;
import com.wynntils.features.inventory.ItemTextOverlayFeature;
import com.wynntils.models.mount.type.ConfigMountStat;
import com.wynntils.utils.render.FontRenderer;
import com.wynntils.utils.render.TextRenderTask;
import com.wynntils.utils.render.type.HorizontalAlignment;
import com.wynntils.utils.render.type.VerticalAlignment;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.DrawContext;
import dev.justfrozt.wyverncolors.compat.FontSizeConfig;
import dev.justfrozt.wyverncolors.compat.PotentialLabelTask;
import dev.justfrozt.wyverncolors.compat.PotentialTextOpacity;
import dev.justfrozt.wyverncolors.config.WyvernSettings;
import dev.justfrozt.wyverncolors.config.PotentialPosition;
import dev.justfrozt.wyverncolors.config.PotentialSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemTextOverlayFeature.class, remap = false)
public abstract class ItemTextOverlaySettingsMixin implements PotentialSettings.Backend {
    @Shadow @Final private Config<ConfigMountStat> mountItemStat;

    @Unique @Persisted(i18nKey = "config.wyverncolors.potentialFontSize")
    private Config<Integer> wyvernPotentialFontSize;
    @Unique @Persisted(i18nKey = "config.wyverncolors.potentialAtBottom")
    private Config<Boolean> wyvernPotentialAtBottom;
    @Unique @Persisted(i18nKey = "config.wyverncolors.potentialPosition")
    private Config<PotentialPosition> wyvernPotentialPosition;
    @Unique @Persisted(i18nKey = "config.wyverncolors.potentialOverride")
    private Config<Boolean> wyvernPotentialOverride;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void wyverncolors$shareSettings(CallbackInfo ci) {
        // Initialize explicitly after the target's own constructor, before native discovery.
        wyvernPotentialFontSize = new FontSizeConfig();
        wyvernPotentialAtBottom = new HiddenConfig<>(true);
        wyvernPotentialPosition = new Config<>(PotentialSettings.DEFAULT_POSITION);
        wyvernPotentialOverride = new Config<>(true);
        PotentialSettings.bind((PotentialSettings.Backend) (Object) this);
    }

    @Unique @Override public int fontSizePercent() { return PotentialSettings.clampSize(wyvernPotentialFontSize.get()); }
    @Unique @Override public PotentialPosition position() { return wyvernPotentialPosition.get(); }
    @Unique @Override public boolean overrideEnabled() { return wyvernPotentialOverride.get(); }
    @Unique @Override public boolean potentialSelected() { return mountItemStat.get() == ConfigMountStat.POTENTIAL; }
    @Unique @Override public void setFontSizePercent(int value) { wyvernPotentialFontSize.setValue(value); wyvernPotentialFontSize.touched(); }
    @Unique @Override public void setPosition(PotentialPosition value) {
        wyvernPotentialPosition.setValue(value); wyvernPotentialPosition.touched();
    }
    @Unique @Override public void migrateLegacyPosition() {
        if (!wyvernPotentialPosition.userEdited() && !wyvernPotentialAtBottom.get()) {
            wyvernPotentialPosition.setValue(PotentialPosition.TOP);
        }
        if (!wyvernPotentialAtBottom.get()) {
            wyvernPotentialAtBottom.reset();
            wyvernPotentialPosition.touched();
        }
    }
    @Unique @Override public void setOverrideEnabled(boolean value) { wyvernPotentialOverride.setValue(value); wyvernPotentialOverride.touched(); }
    @Unique @Override public void resetDefaults() {
        wyvernPotentialFontSize.reset(); wyvernPotentialAtBottom.reset();
        wyvernPotentialPosition.reset(); wyvernPotentialOverride.reset();
        wyvernPotentialOverride.touched();
    }

    @WrapOperation(method = "drawTextOverlay", at = @At(value = "INVOKE",
        desc = @Desc(owner = FontRenderer.class, value = "renderText",
            args = {DrawContext.class, float.class, float.class, TextRenderTask.class})))
    private void wyverncolors$anchorPotential(FontRenderer renderer, DrawContext context,
            float nativeX, float nativeY, TextRenderTask task, Operation<Void> original,
            @Local(argsOnly = true, ordinal = 0) int slotX,
            @Local(argsOnly = true, ordinal = 1) int slotY) {
        var marker = (PotentialLabelTask) (Object) task;
        var layout = marker.wyverncolors$getLayout();
        if (marker.wyverncolors$isPotential()) {
            int opacity = WyvernSettings.current().potentialOpacityPercent();
            if (opacity == 0) return;
            task = PotentialTextOpacity.apply(task, opacity);
        }
        if (layout == null) {
            original.call(renderer, context, nativeX, nativeY, task);
            return;
        }
        // The outer matrix uses the overlay record scale. Ignore native layout settings
        // only for marked potential tasks; all other labels use the original call.
        renderer.renderText(context, task.getText(),
            (slotX + layout.x()) / layout.scale(), (slotY + layout.y()) / layout.scale(),
            task.getSetting().customColor(), HorizontalAlignment.LEFT, VerticalAlignment.TOP,
            task.getSetting().shadow(), 1.0f);
    }
}
