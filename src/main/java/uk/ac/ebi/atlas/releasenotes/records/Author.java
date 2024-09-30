package uk.ac.ebi.atlas.releasenotes.records;

import java.time.Instant;

public record Author(String email, Instant date) {
}
