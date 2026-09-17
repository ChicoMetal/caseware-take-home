package domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Complete change summary for a version range of a template.
 * Used in both collapsed (current -> latest) and step-by-step (consecutive version) views.
 */
public record ChangeSummary(
    int fromVersion,
    int toVersion,
    Instant publishedAt,
    List<SectionChange> sections,
    int totalChanges
) {}
