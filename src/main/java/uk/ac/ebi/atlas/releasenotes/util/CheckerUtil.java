package uk.ac.ebi.atlas.releasenotes.util;

public class CheckerUtil {
    public static String shorten(String text) {
        return text.lines()
                .map(String::trim)
                .map(s -> s.length() > 72 ? s.substring(0, 72 - 3) + "..." : s)
                .findFirst()
                .orElse("(no content)");
    }
}
