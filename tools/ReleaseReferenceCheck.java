import java.nio.file.Path;
import java.util.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Validate production bytecode references, without loading Minecraft classes. */
public final class ReleaseReferenceCheck {
    private final List<JarFile> jars = new ArrayList<>();
    private final Map<String, ClassNode> cache = new HashMap<>();
    private ClassNode read(String name) throws Exception {
        if (cache.containsKey(name)) return cache.get(name);
        for (JarFile jar : jars) {
            var entry = jar.getJarEntry(name + ".class");
            if (entry == null) continue;
            ClassNode node = new ClassNode();
            try (var stream = jar.getInputStream(entry)) { new ClassReader(stream).accept(node, 0); }
            cache.put(name, node); return node;
        }
        try (var stream = ClassLoader.getSystemResourceAsStream(name + ".class")) {
            if (stream != null) {
                ClassNode node = new ClassNode(); new ClassReader(stream).accept(node, 0);
                cache.put(name, node); return node;
            }
        }
        cache.put(name, null); return null;
    }
    private boolean method(String owner, String name, String descriptor, Set<String> seen) throws Exception {
        if (!seen.add(owner)) return false;
        ClassNode type = read(owner);
        if (type == null) return false;
        if (type.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(descriptor))) return true;
        if (name.equals("<init>")) return false;
        if (type.superName != null && method(type.superName, name, descriptor, seen)) return true;
        for (String parent : type.interfaces) if (method(parent, name, descriptor, seen)) return true;
        return false;
    }
    private boolean field(String owner, String name, String descriptor, Set<String> seen) throws Exception {
        if (!seen.add(owner)) return false;
        ClassNode type = read(owner);
        if (type == null) return false;
        if (type.fields.stream().anyMatch(f -> f.name.equals(name) && f.desc.equals(descriptor))) return true;
        if (type.superName != null && field(type.superName, name, descriptor, seen)) return true;
        for (String parent : type.interfaces) if (field(parent, name, descriptor, seen)) return true;
        return false;
    }
    private boolean relevant(String owner) {
        return owner.startsWith("net/minecraft/") || owner.startsWith("net/minecraftforge/")
            || owner.startsWith("net/neoforged/") || owner.startsWith("org/joml/");
    }
    public static void main(String[] args) throws Exception {
        var check = new ReleaseReferenceCheck(); int references = 0;
        try {
            for (String path : args) check.jars.add(new JarFile(Path.of(path).toFile()));
            var entries = check.jars.getFirst().entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement(); String path = entry.getName();
                if (!path.endsWith(".class")) continue;
                ClassNode node = check.read(path.substring(0, path.length() - 6));
                for (MethodNode method : node.methods) {
                    for (var instruction : method.instructions) {
                        if (instruction instanceof MethodInsnNode invoke && check.relevant(invoke.owner)) {
                            if (!check.method(invoke.owner, invoke.name, invoke.desc, new HashSet<>()))
                                throw new IllegalStateException(node.name + ": " + invoke.owner + "." + invoke.name + invoke.desc);
                            references++;
                        } else if (instruction instanceof FieldInsnNode field && check.relevant(field.owner)) {
                            if (!check.field(field.owner, field.name, field.desc, new HashSet<>()))
                                throw new IllegalStateException(node.name + ": " + field.owner + "." + field.name + field.desc);
                            references++;
                        }
                    }
                }
            }
            System.out.println("Verified " + references + " production game/loader/JOML member references");
        } finally { for (JarFile jar : check.jars) jar.close(); }
    }
}
