package domain.model;

import java.time.Instant;
import java.util.List;

public record EngagementUpdateDetails(
    String engagementId,
    int currentVersion,
    int latestVersion,
    ChangeSummary collapsedSummary,
    List<ChangeSummary> stepByStepSummaries,
    Freshness freshness
) {

    public record Freshness(
        Instant computedAt,
        Instant templatePublishedAt
    ) {}
}
