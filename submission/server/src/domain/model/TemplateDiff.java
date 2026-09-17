package domain.model;

import java.time.Instant;
import java.util.List;

public record TemplateDiff(
    String templateId,
    int fromVersion,
    int toVersion,
    Instant generatedAt,
    List<DiffOperation> changes
) {}
