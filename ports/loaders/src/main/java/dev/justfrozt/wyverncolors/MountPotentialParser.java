package dev.justfrozt.wyverncolors;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Reads the potential shown in a mount's original lore. */
public final class MountPotentialParser {
    private static final Pattern POTENTIAL = Pattern.compile(
        "^(\\d{1,3}(?:,\\d{3})+|\\d+(?:[.,]\\d+)?)([kK]?) +Potential$");
    private static final Pattern GROUPED_INTEGER = Pattern.compile("\\d{1,3}(?:,\\d{3})+");
    private static final Pattern MOUNT_NAME = Pattern.compile(".+ (Saddle|Reins|Harness|Whistle|Flute|Ocarina|Horse)$");
    private static final Pattern LEGACY_HORSE = Pattern.compile("(?:Brown |Black |Chestnut |White )?Horse");

    private MountPotentialParser() {}

    public static OptionalInt parse(List<String> lore) {
        return parse(lore, null);
    }

    public static OptionalInt parse(List<String> lore, String serverName) {
        String name = serverName == null ? "" : AppearanceParser.normalize(serverName);
        boolean markedMount = serverName != null && serverName.startsWith("\uDAFC\uDC00")
            && serverName.endsWith("\uDAFC\uDC00") && MOUNT_NAME.matcher(name).matches();
        boolean legacyHorse = LEGACY_HORSE.matcher(name).matches();
        if (!markedMount && !legacyHorse && AppearanceParser.parse(lore).isEmpty()) return OptionalInt.empty();

        OptionalInt potential = OptionalInt.empty();
        // The first modern mount line is an editable title. Legacy horses can start with the value.
        for (int index = legacyHorse ? 0 : 1; index < lore.size(); index++) {
            var number = POTENTIAL.matcher(AppearanceParser.normalize(lore.get(index)));
            if (!number.matches()) continue;
            try {
                String digits = number.group(1);
                boolean abbreviated = !number.group(2).isEmpty();
                String decimal = !abbreviated && GROUPED_INTEGER.matcher(digits).matches()
                    ? digits.replace(",", "") : digits.replace(',', '.');
                int value = new BigDecimal(decimal)
                    .multiply(abbreviated ? BigDecimal.valueOf(1000) : BigDecimal.ONE)
                    .intValueExact();
                if (potential.isPresent() && potential.getAsInt() != value) return OptionalInt.empty();
                potential = OptionalInt.of(value);
            } catch (ArithmeticException | NumberFormatException invalid) {
                return OptionalInt.empty();
            }
        }
        return potential;
    }
}
