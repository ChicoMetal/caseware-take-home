package domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Detail-level API response containing change summaries for an engagement's pending update.
 *
 * @param collapsedSummary    all changes from current version to latest, in a single diff
 * @param stepByStepSummaries changes broken down by consecutive version increments
 */
public record EngagementUpdateDetails(
    String engagementId,
    int currentVersion,
    int latestVersion,
    ChangeSummary collapsedSummary,
    List<ChangeSummary> stepByStepSummaries,
    Freshness freshness
) {

    /** Tracks when this detail view was computed relative to the template's publish date. */
    public record Freshness(
        Instant computedAt,
        Instant templatePublishedAt
    ) {}
}
