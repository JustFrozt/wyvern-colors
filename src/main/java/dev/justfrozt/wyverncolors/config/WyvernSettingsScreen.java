package dev.justfrozt.wyverncolors.config;

import dev.justfrozt.wyverncolors.RenderDiagnostics;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public final class WyvernSettingsScreen extends Screen {
    private final Screen parent;
    private SizeSlider size;
    private OpacitySlider opacity;
    private ButtonWidget swap;
    private ButtonWidget debug;
    private ButtonWidget enabled;
    private ButtonWidget placement;
    private ButtonWidget reset;
    private Text status;
    private int controlsY;
    private int controlsWidth;

    public WyvernSettingsScreen(Screen parent) {
        super(Text.translatable("screen.wyverncolors.potential.title"));
        this.parent = parent;
    }

    @Override protected void init() {
        controlsWidth = Math.min(300, Math.max(100, width - 24));
        int x = (width - controlsWidth) / 2;
        int y = controlsY = Math.max(50, height / 2 - 97);
        swap = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            showSaveResult(WyvernSettings.current().setSwapColors(!WyvernSettings.current().swapColors()));
            refresh();
        }).dimensions(x, y, controlsWidth, 20).build());
        swap.setTooltip(Tooltip.of(Text.translatable("config.wyverncolors.swapColors.description")));
        debug = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            if (showSaveResult(WyvernSettings.current().setDebugLogging(!WyvernSettings.current().debugLogging()))) {
                RenderDiagnostics.settingsChanged();
            }
            refresh();
        }).dimensions(x, y + 24, controlsWidth, 20).build());
        debug.setTooltip(Tooltip.of(Text.translatable("config.wyverncolors.debugLogging.description")));
        enabled = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            var settings = PotentialSettings.backend();
            if (settings != null) settings.setOverrideEnabled(!settings.overrideEnabled());
            refresh();
        }).dimensions(x, y + 48, controlsWidth, 20).build());
        size = addDrawableChild(new SizeSlider(x, y + 72));
        size.setTooltip(Tooltip.of(Text.translatable("config.wyverncolors.potentialFontSize.description")));
        opacity = addDrawableChild(new OpacitySlider(x, y + 96));
        opacity.setTooltip(Tooltip.of(Text.translatable("config.wyverncolors.potentialOpacity.description")));
        placement = addDrawableChild(ButtonWidget.builder(Text.empty(), button -> {
            var settings = PotentialSettings.backend();
            if (settings != null) settings.setPosition(settings.position().next());
            refresh();
        }).dimensions(x, y + 120, controlsWidth, 20).build());
        reset = addDrawableChild(ButtonWidget.builder(Text.translatable("screen.wyverncolors.potential.reset"), button -> {
            var settings = PotentialSettings.backend();
            if (showSaveResult(WyvernSettings.current().resetDefaults())) {
                RenderDiagnostics.settingsChanged();
                if (settings != null) settings.resetDefaults();
            }
            refresh();
        }).dimensions(x, y + 148, (controlsWidth - 8) / 2, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> close())
            .dimensions(x + (controlsWidth + 8) / 2, y + 148, (controlsWidth - 8) / 2, 20).build());
        refresh();
    }

    private boolean showSaveResult(boolean saved) {
        status = saved ? null : Text.translatable("screen.wyverncolors.saveFailed");
        return saved;
    }

    private void refresh() {
        var settings = PotentialSettings.backend();
        boolean available = settings != null;
        boolean on = available && settings.overrideEnabled();
        enabled.active = available;
        reset.active = true;
        size.active = placement.active = on;
        opacity.active = available;
        swap.setMessage(Text.translatable("screen.wyverncolors.swapColors",
            Text.translatable(WyvernSettings.current().swapColors() ? "options.on" : "options.off")));
        debug.setMessage(Text.translatable("screen.wyverncolors.debugLogging",
            Text.translatable(WyvernSettings.current().debugLogging() ? "options.on" : "options.off")));
        enabled.setMessage(Text.translatable("screen.wyverncolors.potential.enabled", Text.translatable(on ? "options.on" : "options.off")));
        placement.setMessage(Text.translatable("screen.wyverncolors.potential.position", Text.translatable(
            (available ? settings.position() : PotentialSettings.DEFAULT_POSITION).translationKey())));
        size.refresh(available ? settings.fontSizePercent() : PotentialSettings.DEFAULT_SIZE);
        opacity.refresh(WyvernSettings.current().potentialOpacityPercent());
    }

    @Override public void tick() { refresh(); }
    @Override public void close() { if (client != null) client.setScreen(parent); }
    @Override public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Text.translatable(PotentialSettings.available()
            ? "screen.wyverncolors.potential.shared" : "screen.wyverncolors.potential.unavailable"),
            width / 2, 37, 0xFFB0B0B0);
        var settings = PotentialSettings.backend();
        if (status == null && settings != null && !settings.potentialSelected()) {
            context.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("screen.wyverncolors.potential.otherStat"), width / 2,
                controlsY + 175, 0xFFB0B0B0);
        }
        if (status != null) {
            context.drawCenteredTextWithShadow(textRenderer, status, width / 2,
                controlsY + 175, 0xFFFF8080);
        }
    }

    private final class SizeSlider extends SliderWidget {
        private SizeSlider(int x, int y) { super(x, y, controlsWidth, 20, Text.empty(), 0); }
        private int percent() { return Math.round(PotentialSettings.MIN_SIZE + (float)value * (PotentialSettings.MAX_SIZE - PotentialSettings.MIN_SIZE)); }
        private void refresh(int percent) {
            value = (double)(percent - PotentialSettings.MIN_SIZE) / (PotentialSettings.MAX_SIZE - PotentialSettings.MIN_SIZE);
            updateMessage();
        }
        @Override protected void updateMessage() {
            setMessage(Text.translatable("screen.wyverncolors.potential.size", percent()));
        }
        @Override protected void applyValue() {
            var settings = PotentialSettings.backend();
            if (settings != null) settings.setFontSizePercent(percent());
        }
    }

    private final class OpacitySlider extends SliderWidget {
        private OpacitySlider(int x, int y) { super(x, y, controlsWidth, 20, Text.empty(), 1); }
        private int percent() { return Math.round((float) value * 100); }
        private void refresh(int percent) { value = percent / 100.0; updateMessage(); }
        @Override protected void updateMessage() {
            setMessage(Text.translatable("screen.wyverncolors.potential.opacity", percent()));
        }
        @Override protected void applyValue() {
            showSaveResult(WyvernSettings.current().setPotentialOpacityPercent(percent()));
        }
    }
}
