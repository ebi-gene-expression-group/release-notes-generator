package uk.ac.ebi.atlas.releasenotes.command;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import uk.ac.ebi.atlas.releasenotes.GitHubClient;
import uk.ac.ebi.atlas.releasenotes.ReleaseNotesApplication;
import uk.ac.ebi.atlas.releasenotes.records.Commit;
import uk.ac.ebi.atlas.releasenotes.records.GitHubProject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@CommandLine.Command(name = "release_notes", mixinStandardHelpOptions = true)
public class ReleaseNoteCommand implements Callable<Integer> {
    private enum OutputFormat {
        MARKDOWN, HTML
    }

    @CommandLine.Option(names = {"-u", "--user"}, description = "GitHub user", required = true)
    private String user;

    @CommandLine.Option(names = {"-r", "--repo"}, description = "GitHub repository", required = true)
    private String repo;

    @CommandLine.Option(names = {"-s", "--since"}, description = "Since commit", required = true)
    private String sinceCommit;

    @CommandLine.Option(names = {"-ut", "--until"}, description = "Until commit", required = true)
    private String untilCommit;

    @CommandLine.Option(names = {"-f", "--file"}, description = "Output file for release notes (optional)")
    private File outputFile;

    @CommandLine.Option(names = {"-v", "--version"}, description = "Release version (optional)", defaultValue = "v1.0.0")
    private String version;

    @CommandLine.Option(names = {"-o", "--output-format"}, description = "Output format (default: MARKDOWN)", defaultValue = "MARKDOWN")
    private OutputFormat outputFormat;

    @Override
    public Integer call() {
        try {
            GitHubProject project = ReleaseNotesApplication.gitHubClient.getProject(user, repo);

            List<Commit> commits = getCommitsInRange(ReleaseNotesApplication.gitHubClient, sinceCommit, untilCommit, user, repo);
            String releaseNotes = generateReleaseNotes(commits, project, version, outputFormat);

            File outputFileWithExtension;
            if (outputFile != null) {
                String extension = (outputFormat == OutputFormat.HTML) ? ".html" : ".md";
                outputFileWithExtension = new File(outputFile.getAbsolutePath() + extension);
                try (PrintWriter writer = new PrintWriter(outputFileWithExtension, StandardCharsets.UTF_8)) {
                    writer.print(releaseNotes);
                    log.info("Release notes saved to: {}", outputFileWithExtension.getAbsolutePath());
                } catch (IOException e) {
                    log.error("Error writing release notes to file: {}", e.getMessage(), e);
                    return 1;
                }
            } else {
                log.info(releaseNotes);
            }

        } catch (Exception e) {
            log.error("Error fetching commits: {}", e.getMessage(), e);
            return 1;
        }
        return 0;
    }

    private String generateReleaseNotes(List<Commit> commits, GitHubProject project, String releaseVersion, OutputFormat format) {
        return switch (format) {
            case MARKDOWN -> generateReleaseNotesMarkdown(commits, project, releaseVersion);
            case HTML -> generateReleaseNotesHTML(commits, project, releaseVersion);
        };
    }

    private String generateReleaseNotesHTML(List<Commit> commits, GitHubProject project, String releaseVersion) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html><head><title>Release Notes for ")
                .append(project.name())
                .append(releaseVersion != null ? " - " + releaseVersion : "")
                .append("</title><style>")
                .append("body { font-family: sans-serif; }")
                .append("h1, h2 { color: #333; }")
                .append("ul { list-style: disc; padding-left: 2em; }")
                .append("li { margin-bottom: 0.5em; }")
                .append("</style></head><body>");

        html.append("<h1>Release Notes for ").append(project.name())
                .append(releaseVersion != null ? " - " + releaseVersion : "")
                .append("</h1>");

        if (project.description() != null && !project.description().isBlank()) {
            html.append("<p><strong>").append(project.description()).append("</strong></p>");
        }

        html.append("<h2>⭐ New Features</h2>");
        html.append("<ul>").append(filterAndFormatCommitsHTML(commits, "Merge")).append("</ul>");

        html.append("<h2>\uD83D\uDC1E Bug Fixes</h2>");
        html.append("<ul>").append(filterAndFormatCommitsHTML(commits, "Fix")).append("</ul>");

        html.append("<h2>\uD83D\uDCA1 Other Changes</h2>");
        html.append("<ul>").append(filterAndFormatCommitsHTML(commits, null)).append("</ul>");

        html.append("</body></html>");
        return html.toString();
    }

    private String filterAndFormatCommitsHTML(List<Commit> commits, String prefix) {
        return commits.stream()
                .filter(commit -> prefix == null || commit.commit().message().startsWith(prefix))
                .map(this::formatCommitHTML)
                .collect(Collectors.joining());
    }

    private String formatCommitHTML(Commit commit) {
        String shortSha = commit.sha().substring(0, 8);
        String commitMessage = commit.commit().message().split("\n")[0];
        String authorEmail = commit.commit().author().email();
        String commitUrl = commit.htmlUrl();

        return String.format("<li><a href='%s'>%s</a> %s (by %s)</li>",
                commitUrl, shortSha, commitMessage, authorEmail);
    }


    private String generateReleaseNotesMarkdown(List<Commit> commits, GitHubProject project, String releaseVersion) {
        StringBuilder markdown = new StringBuilder();

        markdown.append("\n# Release Notes for ").append(project.name())
                .append(releaseVersion != null ? " - " + releaseVersion : "");

        if (project.description() != null && !project.description().isBlank()) {
            markdown.append("\n**").append(project.description()).append("**\n\n");
        }

        markdown.append("##⭐  New Features\n");
        markdown.append(filterAndFormatCommits(commits, "Merge"));

        markdown.append("##\uD83D\uDC1E Bug Fixes\n");
        markdown.append(filterAndFormatCommits(commits, "Fix"));

        markdown.append("##\uD83D\uDCA1 Other Changes\n");
        markdown.append(filterAndFormatCommits(commits, null));

        return markdown.toString();
    }

    private String filterAndFormatCommits(List<Commit> commits, String prefix) {
        return commits.stream()
                .filter(commit -> prefix == null || commit.commit().message().startsWith(prefix))
                .map(this::formatCommitMarkdown)
                .collect(Collectors.joining("\n"));
    }

    private String formatCommitMarkdown(Commit commit) {
        String shortSha = commit.sha().substring(0, 8);
        String commitMessage = commit.commit().message().split("\n")[0];  // First line only
        String authorEmail = commit.commit().author().email();
        String commitUrl = commit.htmlUrl();

        authorEmail = (authorEmail != null) ? authorEmail : "unknown";
        commitUrl = (commitUrl != null) ? commitUrl : "#";

        return String.format("* **[%s](%s)** %s (by %s)", shortSha, commitUrl, commitMessage, authorEmail);
    }


    private List<Commit> getCommitsInRange(@NonNull GitHubClient gitHubClient,
                                           String startCommit,
                                           String endCommit,
                                           String ownerName,
                                           String repoName
    ) {
        List<Commit> commits = gitHubClient.getCommits(ownerName, repoName, endCommit);

        int earliestCommitIdx = findLastIndex(commits, commit -> commit.sha().startsWith(startCommit));
        int latestCommitIdx = findFirstIndex(commits, commit -> commit.sha().startsWith(endCommit));
        if (earliestCommitIdx == -1) {
            throw new IllegalStateException("Couldn't find start commit [%s] in list".formatted(startCommit));
        }

        if (latestCommitIdx > earliestCommitIdx) {
            return List.of();
        }
        return commits.subList(latestCommitIdx, earliestCommitIdx + 1);
    }

    public static <T> int findFirstIndex(List<? extends T> commits, Predicate<? super T> pastStart) {
        for (int i = 0; i < commits.size(); i++) {
            if (pastStart.test(commits.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public static <T> int findLastIndex(List<? extends T> commits, Predicate<? super T> pastEnd) {
        for (int i = commits.size() - 1; i > -1; i--) {
            if (pastEnd.test(commits.get(i))) {
                return i;
            }
        }
        return -1;
    }
}
