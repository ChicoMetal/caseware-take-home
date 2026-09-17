package domain.model;

public record EngagementRecord(
    String engagementId,
    String name,
    String templateId,
    int templateVersion
) {}
