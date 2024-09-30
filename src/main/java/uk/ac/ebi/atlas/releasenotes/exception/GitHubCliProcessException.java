package uk.ac.ebi.atlas.releasenotes.exception;

public class GitHubCliProcessException extends RuntimeException {
    public GitHubCliProcessException(String message) {
        super(message);
    }

    public GitHubCliProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}
