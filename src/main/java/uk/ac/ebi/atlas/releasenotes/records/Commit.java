package uk.ac.ebi.atlas.releasenotes.records;

public record Commit(String sha, CommitDetails commit, String htmlUrl) {

    @Override
    public String toString() {
        return "| %s - %s |".formatted(
                sha.substring(0, 8),
                commit
        );
    }
}
