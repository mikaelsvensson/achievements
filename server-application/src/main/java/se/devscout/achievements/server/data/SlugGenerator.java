package se.devscout.achievements.server.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SlugGenerator {

    public static final Pattern ACCENTED_LATIN_CHARACTER = Pattern.compile("LATIN (SMALL|CAPITAL) LETTER (.) WITH .*");
    public static final Pattern LATIN_CHARACTER = Pattern.compile("LATIN (SMALL|CAPITAL) LETTER (.)");
    public static final Pattern MULTI_WHITE_SPACE = Pattern.compile("[\\s\\n]+");

    public static String toSlug(String text) {
        text = text.trim();
        text = MULTI_WHITE_SPACE.matcher(text).replaceAll("-");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final int codePoint = text.codePointAt(i);
            if (Character.isAlphabetic(codePoint)) {
                final String unicodeCharacterName = Character.getName(codePoint);
                if (LATIN_CHARACTER.matcher(unicodeCharacterName).matches()) {
                    sb.append(Character.toLowerCase(text.charAt(i)));
                } else {
                    final Matcher matcher = ACCENTED_LATIN_CHARACTER.matcher(unicodeCharacterName);
                    if (matcher.matches()) {
                        final String latinLetter = matcher.group(2).toLowerCase();
                        sb.append(latinLetter);
                    }
                }
            } else if (text.charAt(i) == '-') {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }
}
