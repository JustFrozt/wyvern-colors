package dev.justfrozt.wyverncolors;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppearanceParser {
    private static final Pattern PAIR = Pattern.compile(
        "^(azure|cerulean|bronze|ebony|fledge|golden|hollow|infernal|jade|mystic)"
        + " *[-\\u2010\\u2011\\u2012\\u2013\\u2014] *"
        + "(cinder|horn|kander|onyx|quartz|sapphire|rose|shell|ivory|tusk)$",
        Pattern.CASE_INSENSITIVE);

    private AppearanceParser() {}

    public static Optional<Appearance> parse(List<String> rawLore) {
        Appearance result = null;
        // Wynncraft repeats the editable title in line zero. Never read it as appearance.
        for (int i = 1; i < rawLore.size(); i++) {
            Matcher match = PAIR.matcher(normalize(rawLore.get(i)));
            if (!match.matches()) continue;
            Appearance candidate = new Appearance(
                Appearance.Scale.valueOf(match.group(1).toUpperCase(Locale.ROOT)),
                Appearance.Horn.valueOf(match.group(2).toUpperCase(Locale.ROOT)));
            if (result != null && !result.equals(candidate)) return Optional.empty();
            result = candidate;
        }
        return Optional.ofNullable(result);
    }

    /** Historical mappings used by the separate legacy adapters, not the main 1.21.11 renderer. */
    public static boolean isReinsModel(float model) {
        return model == 2790.0f || model == 2791.0f;
    }

    /** Historical flute mapping; current main-adapter mappings live in WyvernItemModels. */
    public static boolean isFluteModel(float model) {
        return model == 2797.0f;
    }

    public static boolean isWyvernModel(float model) {
        return isReinsModel(model) || isFluteModel(model);
    }

    static String normalize(String raw) {
        StringBuilder text = new StringBuilder(raw.length());
        for (int offset = 0; offset < raw.length();) {
            int cp = raw.codePointAt(offset);
            offset += Character.charCount(cp);
            if (cp == '\u00A7' && offset < raw.length()) {
                // Legacy section-sign style codes may coexist with text components.
                offset += Character.charCount(raw.codePointAt(offset));
            } else if (Character.getType(cp) == Character.PRIVATE_USE
                    || isWynncraftSpacingGlyph(cp)) {
                // Strip only decorative font glyphs, not arbitrary punctuation or prose.
            } else if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                text.append(' ');
            } else {
                text.appendCodePoint(cp);
            }
        }
        return text.toString().strip();
    }

    private static boolean isWynncraftSpacingGlyph(int cp) {
        // Exact ranges from the supplied assets/minecraft/font/space.json.
        // These are unassigned Unicode characters, including the U+D0001
        // one-pixel spacer in received appearance lines; PRIVATE_USE misses them.
        return cp == 0xC0001 || (cp >= 0xCF000 && cp <= 0xD1000) || cp == 0xDFFFF;
    }
}
