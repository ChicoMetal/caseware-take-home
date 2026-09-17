package domain.model;

public record HumanReadableChange(
    ChangeType type,
    String description,
    Impact impact
) {}
