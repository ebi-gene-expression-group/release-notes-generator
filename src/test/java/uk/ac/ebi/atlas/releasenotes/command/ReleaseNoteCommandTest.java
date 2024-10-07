package uk.ac.ebi.atlas.releasenotes.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemErr;
import static com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOutNormalized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ReleaseNoteCommandTest {

    private ReleaseNoteCommand releaseNoteCommand;

    public static final String REPO_OWNER = "ebi-gene-expression-group";
    public static final String REPO_NAME = "release-notes-generator";
    public static final String VALID_SHA_SINCE = "afc2ab859d72b0cd8d0ef0076643f789fad6a806";
    public static final String VALID_SHA_UNTIL = "9cf91ff128e1309b52db055b43ece218733f1b17";

    @BeforeEach
    void setUp() {
        releaseNoteCommand = new ReleaseNoteCommand();
    }

    @Test
    void givenInvalidUser_ReturnsNonZeroExitCode() {
        final var cmd = getCommandLine();
        var exitCode = cmd.execute("-u baz");

        assertThat(exitCode).isNotZero();
    }

    @Test
    void givenValidParams_ReturnsZeroExitCode() {
        final var exitCode = executeCommandWithValidParams();

        assertThat(exitCode).isZero();
    }

    @Test
    void givenValidParams_ReturnsCorrectOutput() throws Exception {
        final String title = "Release Notes for " + REPO_NAME;
        final String description = "A helper project to generate release notes for our web apps";
        final String featureSectionTitle = "⭐ New Features";
        final String bugfixSectionTitle = "\uD83D\uDC1E Bug Fixes";
        final String otherChangesSectionTitle = "\uD83D\uDCA1 Other Changes";

        String errText = tapSystemErr(() -> {
            String outText = tapSystemOutNormalized(this::executeCommandWithValidParams);

            assertThat(outText).isNotEmpty();
            assertThat(outText).contains(title);
            assertThat(outText).contains(description);
            assertThat(outText).contains(featureSectionTitle);
            assertThat(outText).contains(bugfixSectionTitle);
            assertThat(outText).contains(otherChangesSectionTitle);
            assertThat(outText).contains(VALID_SHA_SINCE);
            assertThat(outText).contains(VALID_SHA_UNTIL);
        });

        assertEquals("", errText);
    }

    private int executeCommandWithValidParams() {
        final var cmd = getCommandLine();
        return cmd.execute("-u=" + REPO_OWNER,
                "-r=" + REPO_NAME,
                "-s=" + VALID_SHA_SINCE,
                "-ut=" + VALID_SHA_UNTIL
        );
    }

    private CommandLine getCommandLine() {
        return new CommandLine(releaseNoteCommand);
    }
}
