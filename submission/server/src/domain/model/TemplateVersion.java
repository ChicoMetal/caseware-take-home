package domain.model;

import java.time.Instant;

public record TemplateVersion(
    String templateId,
    String displayName,
    int version,
    Instant publishedAt
) {}
