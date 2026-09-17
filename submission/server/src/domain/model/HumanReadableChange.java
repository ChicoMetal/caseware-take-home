package domain.model;

/** A single template change transformed into user-facing language by a {@link domain.port.DiffSummaryTransformer}. */
public record HumanReadableChange(
    ChangeType type,
    String description,
    Impact impact
) {}
