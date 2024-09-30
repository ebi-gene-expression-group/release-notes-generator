package uk.ac.ebi.atlas.releasenotes.records;

import uk.ac.ebi.atlas.releasenotes.util.CheckerUtil;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record CommitDetails(String message, Author author) {
    static final Pattern signedOffByPattern = Pattern.compile("(?:Signed-off-by|Co-authored-by): (.*)$", Pattern.MULTILINE);

    Optional<String> duetCommitSignOffEmail() {
        Matcher matcher = signedOffByPattern.matcher(message);
        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        String shortMessage = CheckerUtil.shorten(message);
        return " %s | %s | <%s>".formatted(shortMessage, author.date(), author.email());
    }
}
