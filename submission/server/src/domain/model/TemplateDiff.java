package domain.model;

import java.time.Instant;
import java.util.List;

/** Raw diff output from the template comparison tool, spanning two versions of a template. */
public record TemplateDiff(
    String templateId,
    int fromVersion,
    int toVersion,
    Instant generatedAt,
    List<DiffOperation> changes
) {}
