package uk.ac.ebi.atlas.releasenotes.records;

public record Commit(String sha, CommitDetails commit, String htmlUrl) {}
