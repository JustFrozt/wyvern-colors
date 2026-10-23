package dev.justfrozt.wyverncolors;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/** A failed appearance never disables another appearance or a newly baked model. */
public final class RenderIsolation<K> {
    private static final int MAX_FAILURES = 512;
    private final Map<K, Object> failures = new LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(Map.Entry<K, Object> entry) {
            return size() > MAX_FAILURES;
        }
    };

    public void render(K key, Object generation, Runnable colored, Runnable original,
            Runnable recoverOriginal, Consumer<RuntimeException> report) {
        Objects.requireNonNull(generation);
        if (failures.get(key) == generation) {
            original.run();
            return;
        }
        RuntimeException failure;
        try {
            colored.run();
            failures.remove(key);
            return;
        } catch (RuntimeException error) {
            failures.put(key, generation);
            failure = error;
        }
        // The normal renderer is outside the catch: its own failures must not be
        // hidden or attributed to custom colors. Recovery rebuilds any partial state.
        report.accept(failure);
        recoverOriginal.run();
    }

    public void clear() { failures.clear(); }
}
