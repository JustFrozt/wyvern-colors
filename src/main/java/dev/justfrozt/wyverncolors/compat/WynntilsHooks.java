package dev.justfrozt.wyverncolors.compat;

import java.util.List;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Check the APIs and injection points without loading any target classes. */
public final class WynntilsHooks {
    private static final String FEATURE = "com/wynntils/features/inventory/ItemTextOverlayFeature";
    private static final String OVERLAY = FEATURE + "$TextOverlay";
    private static final String TASK = "com/wynntils/utils/render/TextRenderTask";
    private static final String STYLE = "com/wynntils/utils/render/TextRenderSetting";
    private static final String FONT = "com/wynntils/utils/render/FontRenderer";
    private static final String STYLED = "com/wynntils/core/text/StyledText";
    private static final String COLOR = "com/wynntils/utils/colors/CustomColor";
    private static final String TYPES = "com/wynntils/utils/render/type/";
    private static final String CONFIG = "com/wynntils/core/persisted/config/Config";

    @FunctionalInterface public interface Reader { ClassNode read(String name) throws Exception; }
    public record Result(boolean supported, String reason) {}

    private WynntilsHooks() {}

    public static Result inspect(Reader reader, String drawContext, String itemStack, String textRenderer) {
        try {
            ClassNode feature = reader.read(FEATURE);
            field(feature, "mountItemStat", "L" + CONFIG + ";");
            method(feature, "<init>", "()V");
            for (String reserved : List.of("wyvernPotentialFontSize", "wyvernPotentialAtBottom",
                    "wyvernPotentialPosition", "wyvernPotentialOverride")) {
                require(feature.fields.stream().noneMatch(f -> f.name.equals(reserved)),
                    "native config field collides with " + reserved);
            }
            for (String reserved : List.of("fontSizePercent", "position", "overrideEnabled", "potentialSelected",
                    "setFontSizePercent", "setPosition", "migrateLegacyPosition", "setOverrideEnabled", "resetDefaults")) {
                require(feature.methods.stream().noneMatch(m -> m.name.equals(reserved)),
                    "native method collides with the potential settings backend: " + reserved);
            }
            MethodNode draw = method(feature, "drawTextOverlay", "(L" + drawContext + ";L" + itemStack + ";IIZ)V");
            call(draw, FONT, "renderText", "(L" + drawContext + ";FFL" + TASK + ";)V", 1);
            call(draw, FEATURE + "$TextOverlayInfo", "getTextOverlay", "()L" + OVERLAY + ";", 1);
            // Do not enable the text integration if an upstream matrix rewrite
            // would scale the coordinates a second time or translate the slot.
            int scales = 0;
            for (var instruction : draw.instructions) {
                if (!(instruction instanceof MethodInsnNode invoke)
                        || !invoke.owner.startsWith("org/joml/Matrix3x2f")) continue;
                require(!invoke.name.startsWith("translate") && !invoke.name.startsWith("rotate"),
                    "item-label matrix has additional placement transforms");
                if (!invoke.name.equals("scale")) continue;
                scales++;
                AbstractInsnNode right = previous(invoke);
                AbstractInsnNode left = previous(previous(right));
                require(isScale(right) && isScale(left), "item-label matrix no longer uses the overlay scale directly");
            }
            require(scales == 1, "item-label matrix scale hook changed");
            ClassNode mount = reader.read(FEATURE + "$MountItemOverlay");
            field(mount, "this$0", "L" + FEATURE + ";");
            MethodNode get = method(mount, "getTextOverlay", "()L" + OVERLAY + ";");
            call(get, OVERLAY, "<init>", "(L" + TASK + ";IIF)V", 1);
            ClassNode overlay = reader.read(OVERLAY);
            method(overlay, "scale", "()F");
            method(overlay, "xOffset", "()I");
            method(overlay, "yOffset", "()I");
            method(overlay, "task", "()L" + TASK + ";");
            ClassNode font = reader.read(FONT);
            method(font, "getInstance", "()L" + FONT + ";");
            method(font, "getFont", "()L" + textRenderer + ";");
            method(font, "renderText", "(L" + drawContext + ";L" + STYLED + ";FFL" + COLOR
                + ";L" + TYPES + "HorizontalAlignment;L" + TYPES + "VerticalAlignment;L"
                + TYPES + "TextShadow;F)V");
            ClassNode task = reader.read(TASK);
            method(task, "getText", "()L" + STYLED + ";");
            method(task, "getSetting", "()L" + STYLE + ";");
            method(task, "<init>", "(L" + STYLED + ";L" + STYLE + ";)V");
            ClassNode style = reader.read(STYLE);
            method(style, "customColor", "()L" + COLOR + ";");
            method(style, "withCustomColor", "(L" + COLOR + ";)L" + STYLE + ";");
            method(reader.read(COLOR), "asInt", "()I");
            method(reader.read(COLOR), "withAlpha", "(I)L" + COLOR + ";");
            method(style, "shadow", "()L" + TYPES + "TextShadow;");
            method(reader.read("com/wynntils/core/persisted/config/ConfigManager"), "loadConfigOptions", "(ZZ)V");
            method(reader.read(CONFIG), "setValue", "(Ljava/lang/Object;)V");
            method(reader.read(CONFIG), "reset", "()V");
            method(reader.read("com/wynntils/core/persisted/config/HiddenConfig"), "<init>", "(Ljava/lang/Object;)V");
            field(reader.read("com/wynntils/models/mount/type/ConfigMountStat"), "POTENTIAL",
                "Lcom/wynntils/models/mount/type/ConfigMountStat;");
            return new Result(true, "verified mount layout and native config hooks");
        } catch (Exception | LinkageError failure) {
            return new Result(false, failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage());
        }
    }

    private static AbstractInsnNode previous(AbstractInsnNode instruction) {
        if (instruction == null) return null;
        instruction = instruction.getPrevious();
        while (instruction != null && instruction.getOpcode() < 0) instruction = instruction.getPrevious();
        return instruction;
    }
    private static boolean isScale(AbstractInsnNode instruction) {
        return instruction instanceof MethodInsnNode invoke && invoke.owner.equals(OVERLAY)
            && invoke.name.equals("scale") && invoke.desc.equals("()F");
    }
    private static void field(ClassNode node, String name, String descriptor) {
        require(node.fields.stream().anyMatch(field -> field.name.equals(name) && field.desc.equals(descriptor)),
            node.name + "." + name + " field changed");
    }
    private static MethodNode method(ClassNode node, String name, String descriptor) {
        return node.methods.stream().filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
            .findFirst().orElseThrow(() -> new IllegalStateException(node.name + "." + name + " signature changed"));
    }
    private static void call(MethodNode node, String owner, String name, String descriptor, int count) {
        int found = 0;
        for (var instruction : node.instructions) {
            if (instruction instanceof MethodInsnNode invoke && invoke.owner.equals(owner)
                    && invoke.name.equals(name) && invoke.desc.equals(descriptor)) found++;
        }
        require(found == count, node.name + ": " + owner + "." + name + " injection point changed");
    }
    private static void require(boolean condition, String reason) {
        if (!condition) throw new IllegalStateException(reason);
    }
}
