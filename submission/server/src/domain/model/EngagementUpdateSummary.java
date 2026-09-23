package domain.model;

import java.time.Instant;

/**
 * List-level API response showing an engagement's template update status.
 *
 * @param declinedVersion the version the user explicitly declined; {@code null} when no
 *                        decline is on record. A decline is superseded when a newer template
 *                        version is published (status reverts to PENDING).
 */
public record EngagementUpdateSummary(
    String engagementId,
    String firmId,
    String engagementName,
    String templateId,
    String templateDisplayName,
    int currentVersion,
    int latestVersion,
    UpdateStatus status,
    int pendingUpdateCount,
    boolean summaryAvailable,
    Instant lastCheckedAt,
    Integer declinedVersion
) {}
