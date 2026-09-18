package domain.model;

/**
 * Lightweight engagement index entry (CQRS read model).
 * Avoids loading the full engagement file (~1 minute) by caching only the
 * fields needed for update status resolution.
 *
 * <p>Status fields ({@code status}, {@code latestVersion}, {@code declinedVersion},
 * {@code summaryAvailable}) are materialized by {@link domain.service.TemplateUpdateProcessor}
 * when a template is published, and by decision processing when a user applies or declines.
 */
public record EngagementRecord(
    String engagementId,
    String firmId,
    String name,
    String templateId,
    int templateVersion,
    int latestVersion,
    UpdateStatus status,
    Integer declinedVersion,
    boolean summaryAvailable
) {}
