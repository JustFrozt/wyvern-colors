import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Check remapped injection references against the game and optional mod JARs. */
public final class MixinTargetCheck {
    private final List<JarFile> jars = new ArrayList<>();
    private int mixins;
    private int references;
    private int calls;

    private ClassNode read(String name) throws Exception {
        for (JarFile jar : jars) {
            var entry = jar.getJarEntry(name.replace('.', '/') + ".class");
            if (entry == null) continue;
            ClassNode node = new ClassNode();
            try (var stream = jar.getInputStream(entry)) {
                new ClassReader(stream).accept(node, 0);
            }
            return node;
        }
        return null;
    }

    private static Object value(AnnotationNode annotation, String key) {
        if (annotation.values == null) return null;
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(key)) return annotation.values.get(i + 1);
        }
        return null;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> values ? values : value == null ? List.of() : List.of(value);
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<AnnotationNode> result = new ArrayList<>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }

    private boolean method(ClassNode node, String name, String descriptor) throws Exception {
        if (node == null) return false;
        if (node.methods.stream().anyMatch(m -> m.name.equals(name)
                && (descriptor == null || m.desc.equals(descriptor)))) return true;
        if (node.superName != null && method(read(node.superName), name, descriptor)) return true;
        for (String parent : node.interfaces) if (method(read(parent), name, descriptor)) return true;
        return false;
    }

    private void require(boolean condition, String reference) {
        if (!condition) throw new IllegalStateException("Missing remapped target: " + reference);
        references++;
    }

    private void checkAt(Object at, List<MethodNode> methods) throws Exception {
        for (Object entry : list(at)) {
            if (!(entry instanceof AnnotationNode annotation)) continue;
            Object raw = value(annotation, "target");
            if (!(raw instanceof String target) || !target.startsWith("L") || !target.contains(";")) continue;
            int separator = target.indexOf(';');
            String owner = target.substring(1, separator);
            ClassNode node = read(owner);
            if (node == null) {
                if (owner.startsWith("net/minecraft/")) {
                    throw new IllegalStateException("Missing game class: " + owner);
                }
                continue;
            }
            String member = target.substring(separator + 1);
            int parameters = member.indexOf('(');
            if (parameters >= 0) {
                require(method(node, member.substring(0, parameters), member.substring(parameters)), target);
                if ("INVOKE".equals(value(annotation, "value")) && !methods.isEmpty()) {
                    int matches = 0;
                    for (MethodNode selected : methods) {
                        for (var instruction : selected.instructions) {
                            if (instruction instanceof MethodInsnNode invoke
                                    && invoke.owner.equals(owner)
                                    && invoke.name.equals(member.substring(0, parameters))
                                    && invoke.desc.equals(member.substring(parameters))) matches++;
                        }
                    }
                    Object ordinal = value(annotation, "ordinal");
                    int minimum = ordinal instanceof Integer index && index >= 0 ? index + 1 : 1;
                    require(matches >= minimum, "call site " + target);
                    calls++;
                }
            }
        }
    }

    private void check(ClassNode mixin) throws Exception {
        AnnotationNode declaration = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream()
            .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")).findFirst().orElse(null);
        if (declaration == null) return;
        List<String> targets = new ArrayList<>();
        for (Object type : list(value(declaration, "value"))) targets.add(((Type) type).getInternalName());
        for (Object type : list(value(declaration, "targets"))) targets.add(type.toString().replace('.', '/'));
        for (String target : targets) {
            ClassNode node = read(target);
            if (node == null && target.startsWith("com/wynntils/")) continue;
            require(node != null, target);
            mixins++;
            for (var field : mixin.fields) {
                for (var annotation : annotations(field.visibleAnnotations, field.invisibleAnnotations)) {
                    if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")) {
                        require(node.fields.stream().anyMatch(f -> f.name.equals(field.name)
                            && f.desc.equals(field.desc)), target + "." + field.name + field.desc);
                    }
                }
            }
            for (var injected : mixin.methods) {
                for (var annotation : annotations(injected.visibleAnnotations, injected.invisibleAnnotations)) {
                    List<MethodNode> selected = new ArrayList<>();
                    if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")) {
                        require(method(node, injected.name, injected.desc), target + "." + injected.name + injected.desc);
                    }
                    if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/gen/Accessor;")) {
                        String field = String.valueOf(value(annotation, "value"));
                        require(node.fields.stream().anyMatch(f -> f.name.equals(field)), target + "." + field);
                    }
                    for (Object raw : list(value(annotation, "method"))) {
                        String selector = raw.toString();
                        int parameters = selector.indexOf('(');
                        require(method(node, parameters < 0 ? selector : selector.substring(0, parameters),
                            parameters < 0 ? null : selector.substring(parameters)), target + "." + selector);
                        for (MethodNode candidate : node.methods) {
                            if (candidate.name.equals(parameters < 0 ? selector : selector.substring(0, parameters))
                                    && (parameters < 0 || candidate.desc.equals(selector.substring(parameters)))) {
                                selected.add(candidate);
                            }
                        }
                    }
                    checkAt(value(annotation, "at"), selected);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("MOD_JAR GAME_JAR [DEPENDENCY_JAR ...]");
        var checker = new MixinTargetCheck();
        try {
            for (String path : args) checker.jars.add(new JarFile(Path.of(path).toFile()));
            var entries = checker.jars.getFirst().entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("dev/justfrozt/wyverncolors/mixin/") && name.endsWith(".class")) {
                    checker.check(checker.read(name.substring(0, name.length() - 6)));
                }
            }
            System.out.printf("Verified %d mixins, %d remapped references, and %d invocation points%n",
                checker.mixins, checker.references, checker.calls);
        } finally {
            for (JarFile jar : checker.jars) jar.close();
        }
    }
}
