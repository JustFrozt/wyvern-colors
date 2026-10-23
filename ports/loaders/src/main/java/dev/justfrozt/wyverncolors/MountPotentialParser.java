package dev.justfrozt.wyverncolors;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Reads server-provided mount lore without a dependency on an optional mod. */
public final class MountPotentialParser {
    private static final Pattern POTENTIAL = Pattern.compile("^(\\d+(?:\\.\\d+)?)([kK]?) +Potential$");
    private static final Pattern MOUNT_NAME = Pattern.compile(".+ (Saddle|Reins|Harness|Whistle|Flute|Ocarina)$");
    private static final Pattern STAT = Pattern.compile(
        "^(Speed|Acceleration|Jump Height|Altitude|Energy|Handling|Toughness|Boost|Training)"
        + "\\s*:?\\s*(\\d+)\\s*/\\s*(\\d+)(?:\\s*\\((\\d+)\\))?\\s*$");

    private MountPotentialParser() {}

    public static OptionalInt parse(List<String> lore) {
        return parse(lore, null);
    }

    public static OptionalInt parse(List<String> lore, String serverName) {
        // Do not mistake a renamed ordinary item or the repeated editable title for a mount.
        // Wynncraft's marked mount names also allow text to survive unknown/new appearance colors.
        boolean markedMount = serverName != null && serverName.startsWith("\uDAFC\uDC00")
            && serverName.endsWith("\uDAFC\uDC00") && MOUNT_NAME.matcher(AppearanceParser.normalize(serverName)).matches();
        if (!markedMount && AppearanceParser.parse(lore).isEmpty()) return OptionalInt.empty();
        Integer potential = null;
        Map<String, Integer> maxima = new HashMap<>();
        for (int index = 1; index < lore.size(); index++) {
            String raw = lore.get(index);
            String line = AppearanceParser.normalize(raw);
            var number = POTENTIAL.matcher(line);
            try {
                if (number.matches()) {
                    int value = new BigDecimal(number.group(1))
                        .multiply(number.group(2).isEmpty() ? BigDecimal.ONE : BigDecimal.valueOf(1000))
                        .intValueExact();
                    if (potential != null && potential != value) return OptionalInt.empty();
                    potential = value;
                    continue;
                }
                var stat = STAT.matcher(line);
                if (!stat.matches()) continue;
                int cap = Integer.parseInt(stat.group(3));
                int maximum = stat.group(4) == null ? estimateMaximum(raw, cap)
                    : Integer.parseInt(stat.group(4));
                // The energy header has its meter before the word Energy, unlike stat rows.
                int word = raw.indexOf(stat.group(1));
                if (stat.group(1).equals("Energy") && raw.substring(0, Math.max(0, word))
                        .codePoints().anyMatch(cp -> cp == 0xE023)) continue;
                Integer previous = maxima.put(stat.group(1), maximum);
                if (previous != null && previous != maximum) return OptionalInt.empty();
            } catch (ArithmeticException | NumberFormatException invalid) {
                return OptionalInt.empty();
            }
        }
        if (potential == null) return OptionalInt.empty();
        // Match the default Wynntils estimated-potential display when stat maxima are present.
        if (!maxima.isEmpty()) {
            try {
                int sum = 0;
                for (int maximum : maxima.values()) sum = Math.addExact(sum, maximum);
                return sum > 0 ? OptionalInt.of(sum) : OptionalInt.of(potential);
            } catch (ArithmeticException overflow) { return OptionalInt.empty(); }
        }
        return OptionalInt.of(potential);
    }

    private static int estimateMaximum(String raw, int cap) {
        int ratio = raw.indexOf('/');
        int background = raw.indexOf('\uE023', Math.max(0, ratio));
        if (background < 0) return cap;
        int count = 0, stage = 0;
        for (int cp : raw.substring(background + 1).codePoints().toArray()) {
            if (cp >= 0xE000 && cp <= 0xE023) { count++; stage = cp - 0xE000 + 1; }
            else if (cp >= 0xF000 && cp <= 0xF023) { count++; stage = cp - 0xF000 + 1; }
            else if (cp == 0xD0009) { count++; stage = 0; }
        }
        // Current meter (including an empty spacer), then optional maximum meter.
        if (count < 2 || stage <= 0 || stage >= 36) return cap;
        return Math.round(cap / (1f - stage / 36f));
    }

    public static int color(int potential) {
        if (potential >= 1500) return 0xFF55FFFF;
        if (potential >= 1185) return 0xFF55FF55;
        if (potential >= 870) return 0xFFFFFF55;
        if (potential >= 555) return 0xFFFFAA00;
        return 0xFFFF5555;
    }
}
