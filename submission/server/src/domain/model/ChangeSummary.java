package domain.model;

import java.time.Instant;
import java.util.List;

public record ChangeSummary(
    int fromVersion,
    int toVersion,
    Instant publishedAt,
    List<SectionChange> sections,
    int totalChanges
) {}
