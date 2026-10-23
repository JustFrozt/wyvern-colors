package dev.justfrozt.wyverncolors;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

/** Cache appearance data only; roles come from the native model selected this draw. */
public final class MountAppearanceResolver {
    public enum Status { SUPPORTED, UNRELATED, UNSUPPORTED_MODEL, UNSUPPORTED_APPEARANCE }
    public record Resolution(Status status, Float model, Optional<Appearance> appearance,
                             String appearanceHint) {}
    private static final Map<ItemStack, Cached> CACHE = new WeakHashMap<>();
    private static final Pattern APPEARANCE_LINE = Pattern.compile(
        "^[\\p{L}]+ *[-\\u2010\\u2011\\u2012\\u2013\\u2014] *[\\p{L}]+$");
    private record Cached(LoreComponent lore, CustomModelDataComponent data,
                          Optional<Appearance> appearance, boolean mountLore, String hint) {}
    private MountAppearanceResolver() {}

    public static Optional<Appearance> resolve(ItemStack stack, WyvernItemModels.Role role) {
        Resolution result = inspect(stack, role);
        return result.status() == Status.SUPPORTED ? result.appearance() : Optional.empty();
    }

    public static Resolution inspect(ItemStack stack, WyvernItemModels.Role role) {
        CustomModelDataComponent data = stack.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        Cached cached = CACHE.get(stack);
        if (cached == null || cached.lore() != lore || cached.data() != data) {
            List<String> lines = lore == null ? List.of() : lore.lines().stream().map(Text::getString).toList();
            Optional<Appearance> appearance = AppearanceParser.parse(lines);
            boolean mountLore = lines.stream().skip(1).anyMatch(line -> line.contains("Potential"));
            String hint = appearance.isPresent() ? "" : lines.stream().skip(1)
                .map(AppearanceParser::normalize)
                .filter(line -> line.length() <= 96 && APPEARANCE_LINE.matcher(line).matches())
                .limit(3).collect(java.util.stream.Collectors.joining(", "));
            cached = new Cached(lore, data, appearance, mountLore, hint);
            CACHE.put(stack, cached);
        }
        Status status = role != WyvernItemModels.Role.NONE
            ? (cached.appearance().isPresent() ? Status.SUPPORTED : Status.UNSUPPORTED_APPEARANCE)
            : (cached.appearance().isPresent() && cached.mountLore() ? Status.UNSUPPORTED_MODEL : Status.UNRELATED);
        // This float is diagnostic information, never a classification rule.
        Float model = data == null ? null : data.getFloat(0);
        return new Resolution(status, model, cached.appearance(),
            status == Status.UNSUPPORTED_APPEARANCE ? cached.hint() : "");
    }

    /** Only the private copy receives colors; no base item or numeric ID is assumed. */
    public static ItemStack renderCopy(ItemStack original, Appearance appearance,
            WyvernItemModels.Role role, boolean swapColors) {
        ItemStack copy = original.copy();
        CustomModelDataComponent data = original.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        copy.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(
            data == null ? List.of() : data.floats(), data == null ? List.of() : data.flags(),
            data == null ? List.of() : data.strings(),
            role == WyvernItemModels.Role.FLUTE ? appearance.fluteTints() : appearance.modelTints(swapColors)));
        return copy;
    }

    public static void resourcesReloaded() { CACHE.clear(); }
}
