package domain.model;

/**
 * Lightweight engagement index entry (CQRS read model).
 * Avoids loading the full engagement file (~1 minute) by caching only the
 * fields needed for update status resolution.
 */
public record EngagementRecord(
    String engagementId,
    String firmId,
    String name,
    String templateId,
    int templateVersion
) {}
