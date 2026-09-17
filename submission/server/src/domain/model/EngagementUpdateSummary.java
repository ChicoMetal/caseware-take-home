package domain.model;

import java.time.Instant;

public record EngagementUpdateSummary(
    String engagementId,
    String engagementName,
    String templateId,
    String templateDisplayName,
    int currentVersion,
    int latestVersion,
    UpdateStatus status,
    int pendingUpdateCount,
    boolean summaryAvailable,
    Instant lastCheckedAt
) {}
