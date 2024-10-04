package uk.ac.ebi.atlas.releasenotes.records;

import java.time.OffsetDateTime;

public record GitHubProject(String defaultBranch,
                            String name,
                            String description,
                            String htmlUrl,
                            OffsetDateTime updatedAt) {}
