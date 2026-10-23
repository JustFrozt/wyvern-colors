package dev.justfrozt.wyverncolors;

import dev.justfrozt.wyverncolors.config.WyvernSettings;
import dev.justfrozt.wyverncolors.mixin.CompositeItemModelAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.client.render.item.model.CompositeItemModel;
import net.minecraft.client.render.item.model.EmptyItemModel;
import net.minecraft.client.render.item.model.ItemModel;
import net.minecraft.client.render.item.model.MissingItemModel;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Wynncraft inventory and held draws. Each selection has its own failure boundary. */
public final class GuiColorRenderer {
    @FunctionalInterface public interface Reporter {
        void report(String code, String identity, String details, RuntimeException error);
    }
    private record Key(Appearance appearance, Identifier asset) {}
    private record CheckedAssets(ItemModel generation, String missing) {}
    private static final RenderIsolation<Key> ISOLATION = new RenderIsolation<>();
    private static final Map<Identifier, CheckedAssets> CHECKED = new HashMap<>();
    private static final Identifier WHITE = id("textures/item/white.png");
    private static final Identifier SPARKLES = id("models/item/sparkles.json");
    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();

    private static final class Frame {
        final ItemRenderState state;
        final ItemStack stack;
        final Function<Identifier, ItemModel> models;
        final Predicate<Identifier> resources;
        final Reporter reporter;
        boolean recognized;
        boolean recovering;
        Frame(ItemRenderState state, ItemStack stack, Function<Identifier, ItemModel> models,
                Predicate<Identifier> resources, Reporter reporter) {
            this.state = state; this.stack = stack; this.models = models;
            this.resources = resources; this.reporter = reporter;
        }
    }

    /** Only a failed custom update unwinds the surrounding native composite. */
    private static final class RestartOriginal extends RuntimeException {
        RestartOriginal() { super(null, null, false, false); }
    }

    private GuiColorRenderer() {}
    private static Identifier id(String path) { return Identifier.of("wyverncolors", path); }

    public static boolean recovering() {
        Frame frame = ACTIVE.get();
        return frame != null && frame.recovering;
    }

    public static void renderRoot(ItemRenderState state, ItemStack stack,
            Function<Identifier, ItemModel> models, Predicate<Identifier> resources,
            Runnable nativeUpdate, Runnable recoverOriginal, Reporter reporter) {
        renderRoot(state, stack, models, resources, nativeUpdate, recoverOriginal, reporter, true);
    }

    public static void renderRoot(ItemRenderState state, ItemStack stack,
            Function<Identifier, ItemModel> models, Predicate<Identifier> resources,
            Runnable nativeUpdate, Runnable recoverOriginal, Reporter reporter, boolean reportUnrecognized) {
        Frame previous = ACTIVE.get();
        Frame frame = new Frame(state, stack, models, resources, reporter);
        ACTIVE.set(frame);
        try {
            try {
                nativeUpdate.run();
            } catch (RestartOriginal restart) {
                frame.recovering = true;
                recoverOriginal.run();
            }
            // A pack may intentionally select empty for a held mount. GUI draws
            // still diagnose unsupported pack models without warning on that branch.
            if (!frame.recognized && reportUnrecognized) {
                try {
                    var result = MountAppearanceResolver.inspect(stack, WyvernItemModels.Role.NONE);
                    reportUnsupportedModel(stack, result, reporter);
                } catch (RuntimeException error) {
                    reporter.report("item-data", "inspection", "Could not read an item's appearance data", error);
                }
            }
        } finally {
            if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        }
    }

    public static void renderSelected(WyvernItemModels.Role role, ItemRenderState state,
            ItemStack stack, ItemModel vanilla, BiConsumer<ItemModel, ItemStack> update) {
        Frame frame = ACTIVE.get();
        if (frame == null || frame.recovering || frame.state != state || frame.stack != stack) {
            update.accept(vanilla, stack);
            return;
        }
        frame.recognized = true;
        render(stack, role, vanilla, frame.models, frame.resources, update,
            () -> { throw new RestartOriginal(); }, frame.reporter);
    }

    private static void reportUnsupportedModel(ItemStack stack,
            MountAppearanceResolver.Resolution result, Reporter reporter) {
        if (result.status() != MountAppearanceResolver.Status.UNSUPPORTED_MODEL) return;
        String identity = stack.get(DataComponentTypes.ITEM_MODEL) + "|" + result.model();
        reporter.report("unsupported-model", identity,
            "Wyvern appearance " + result.appearance().orElseThrow().key()
            + " did not select a recognized wyvern model in the active pack"
            + " (item model " + stack.get(DataComponentTypes.ITEM_MODEL)
            + ", first custom-model float " + result.model() + ")", null);
    }

    public static void render(ItemStack stack, WyvernItemModels.Role role, ItemModel vanilla,
            Function<Identifier, ItemModel> models, Predicate<Identifier> resourcePresent,
            BiConsumer<ItemModel, ItemStack> original, Runnable recoverOriginal, Reporter reporter) {
        Runnable normal = () -> original.accept(vanilla, stack);
        MountAppearanceResolver.Resolution resolution;
        try {
            resolution = MountAppearanceResolver.inspect(stack, role);
        } catch (RuntimeException error) {
            reporter.report("item-data", "inspection", "Could not read an item's appearance data", error);
            normal.run();
            return;
        }
        if (resolution.status() != MountAppearanceResolver.Status.SUPPORTED) {
            if (resolution.status() == MountAppearanceResolver.Status.UNSUPPORTED_MODEL) {
                reportUnsupportedModel(stack, resolution, reporter);
            } else if (resolution.status() == MountAppearanceResolver.Status.UNSUPPORTED_APPEARANCE) {
                reporter.report("appearance", resolution.model() + "|" + resolution.appearanceHint(),
                    "Model " + resolution.model() + " has missing, unknown, or conflicting appearance lore"
                    + (resolution.appearanceHint().isEmpty() ? "" : ": " + resolution.appearanceHint()), null);
            }
            normal.run();
            return;
        }

        Appearance appearance = resolution.appearance().orElseThrow();
        boolean flute = role == WyvernItemModels.Role.FLUTE;
        boolean swapped = !flute && WyvernSettings.current().swapColors();
        String path = flute ? "flute" : "saddle" + (appearance.isMystic() ? "_mystic" : "")
            + (swapped ? "_swapped" : "");
        Identifier asset = id(path);
        ItemModel colored;
        ItemStack renderCopy;
        String unavailable;
        try {
            colored = models.apply(asset);
            if (missingModel(colored, 0)) {
                unavailable = "Custom icon " + asset + " or one of its layers is missing or empty";
            } else {
                String missing = missingResource(asset, colored, flute, swapped, appearance.isMystic(), resourcePresent);
                unavailable = missing == null ? null : "Custom icon " + asset
                    + " is missing required resource " + missing;
            }
            renderCopy = unavailable == null ? MountAppearanceResolver.renderCopy(stack, appearance, role, swapped) : null;
        } catch (RuntimeException error) {
            reporter.report("item-data", asset + "|" + appearance.key(),
                "Could not prepare " + appearance.key() + " for custom icon " + asset, error);
            // Only preparation is guarded. Never catch a failure from the original renderer.
            normal.run();
            return;
        }
        if (unavailable != null) {
            reporter.report("model-assets", asset.toString(), unavailable, null);
            normal.run();
            return;
        }
        RenderDiagnostics.modelSelected(appearance, asset, vanilla, colored);
        ISOLATION.render(new Key(appearance, asset), colored,
            () -> original.accept(colored, renderCopy), normal, recoverOriginal,
            error -> reporter.report("render", asset + "|" + appearance.key(),
                "Could not render " + appearance.key() + " using " + asset, error));
    }

    private static boolean missingModel(ItemModel model, int depth) {
        if (model == null || model instanceof MissingItemModel || model instanceof EmptyItemModel || depth > 8) return true;
        if (model instanceof CompositeItemModel) {
            if (!(model instanceof CompositeItemModelAccessor composite)) return true;
            var children = composite.wyverncolors$getModels();
            return children.isEmpty() || children.stream().anyMatch(child -> missingModel(child, depth + 1));
        }
        return false;
    }

    private static String missingResource(Identifier asset, ItemModel generation, boolean flute,
            boolean swapped, boolean mystic, Predicate<Identifier> present) {
        CheckedAssets previous = CHECKED.get(asset);
        if (previous != null && previous.generation() == generation) return previous.missing();
        String geometry = flute ? "flute" : (swapped ? "saddle_swapped" : "saddle");
        String missing = null;
        for (Identifier resource : List.of(id("items/" + asset.getPath() + ".json"),
                id("models/item/" + geometry + ".json"), WHITE)) {
            if (!present.test(resource)) { missing = resource.toString(); break; }
        }
        if (missing == null && !flute && mystic && !present.test(SPARKLES)) missing = SPARKLES.toString();
        CHECKED.put(asset, new CheckedAssets(generation, missing));
        return missing;
    }

    public static void resourcesReloaded() {
        CHECKED.clear(); ISOLATION.clear(); MountAppearanceResolver.resourcesReloaded();
    }
}
