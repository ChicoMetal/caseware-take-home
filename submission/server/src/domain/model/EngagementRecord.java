package domain.model;

public record EngagementRecord(
    String engagementId,
    String firmId,
    String name,
    String templateId,
    int templateVersion
) {}
