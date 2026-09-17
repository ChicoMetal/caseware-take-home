package domain.model;

import java.time.Instant;

/** A published version of a product template available for engagement use. */
public record TemplateVersion(
    String templateId,
    String displayName,
    int version,
    Instant publishedAt
) {}
