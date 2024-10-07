package uk.ac.ebi.atlas.releasenotes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static uk.ac.ebi.atlas.releasenotes.ReleaseNotesApplication.getApiToken;

class GitHubClientTest {

    private GitHubClient gitHubClient;

    public static final String REPO_OWNER = "ebi-gene-expression-group";
    public static final String REPO_NAME = "release-notes-generator";
    public static final String INVALID_REPOSITORY_OWNER = "foo";
    public static final String INVALID_REPOSITORY = "barbaz";
    public static final String VALID_SHA_FOR_GIVEN_REPO = "9cf91ff128e1309b52db055b43ece218733f1b17";

    @BeforeEach
    void setUp() {
        var objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        gitHubClient = Feign.builder()
                    .decoder(new JacksonDecoder(objectMapper))
                    .encoder(new JacksonEncoder(objectMapper))
                    .requestInterceptor(request ->
                            request.header("Authorization", "Bearer " + getApiToken()))
                    .target(GitHubClient.class, "https://api.github.com");
    }

    @Test
    void gettingANonExistentProject_returnsANotFoundStatus()  {
        final String status = "status";
        final int notFound = 404;
        final String statusMessageStart = "[404 Not Found";

        assertThatExceptionOfType(FeignException.NotFound.class)
                .isThrownBy(() -> gitHubClient.getProject(
                        INVALID_REPOSITORY_OWNER, INVALID_REPOSITORY + generateRandomNumberWithLength(16))
        )
                .hasFieldOrPropertyWithValue(status, notFound)
                .withMessageStartingWith(statusMessageStart);
    }

    @Test
    void gettingAnExistentProject_returnsProjectInformation()  {
        var expectedBranchName = "main";
        var expectedURL = String.format("https://github.com/%s/%s", REPO_OWNER, REPO_NAME);

        var project = gitHubClient.getProject(REPO_OWNER, REPO_NAME);

        assertThat(project.name())
                .isEqualTo(REPO_NAME);
        assertThat(project.defaultBranch())
                .isEqualTo(expectedBranchName);
        assertThat(project.htmlUrl())
                .isEqualTo(expectedURL);
    }

    @Test
    void givenInvalidParams_returnsANotFoundStatus() {
        final String status = "status";
        final int notFound = 404;
        final String statusMessageStart = "[404 Not Found";

        assertThatExceptionOfType(FeignException.NotFound.class)
                .isThrownBy(() -> gitHubClient.getCommits(INVALID_REPOSITORY_OWNER, INVALID_REPOSITORY, getSha()))
                .hasFieldOrPropertyWithValue(status, notFound)
                .withMessageStartingWith(statusMessageStart);
    }

    @Test
    void givenValidParams_returnsCommitsPageInformation() {
        var pageNumber = 1;
        var commits = gitHubClient.getCommitsPage(REPO_OWNER, REPO_NAME, VALID_SHA_FOR_GIVEN_REPO, pageNumber);

        assertThat(commits.size()).isGreaterThan(0);

        commits.forEach(commit -> {
           assertThat(commit.sha()).isNotEmpty();
           assertThat(commit.commit()).isNotNull();
           assertThat(commit.htmlUrl()).isNotEmpty();
        });
    }

    @Test
    void givenInvalidParams_getCommitsReturnsANotFoundStatus() {
        final String status = "status";
        final int notFound = 404;
        final String statusMessageStart = "[404 Not Found";

        assertThatExceptionOfType(FeignException.NotFound.class)
                .isThrownBy(() -> gitHubClient.getCommits(INVALID_REPOSITORY_OWNER, INVALID_REPOSITORY, getSha()))
                .hasFieldOrPropertyWithValue(status, notFound)
                .withMessageStartingWith(statusMessageStart);
    }

    @Test
    void givenValidParams_returnsListOfCommits() {
        var commits = gitHubClient.getCommits(REPO_OWNER, REPO_NAME, VALID_SHA_FOR_GIVEN_REPO);

        assertThat(commits.size()).isGreaterThan(0);

        commits.forEach(commit -> {
            assertThat(commit.sha()).isNotEmpty();
            assertThat(commit.commit()).isNotNull();
            assertThat(commit.htmlUrl()).isNotEmpty();
        });
    }

    private String getSha() {
        return generateRandomNumberWithLength(8);
    }

    private String generateRandomNumberWithLength(int length) {
        return Stream.generate(this::generateRandomNumber)
                .limit(length)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private String generateRandomNumber() {
        return String.valueOf(new Random(Long.MAX_VALUE).nextInt(10));
    }
}
