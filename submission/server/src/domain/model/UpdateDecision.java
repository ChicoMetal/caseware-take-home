package domain.model;

public record UpdateDecision(
    DecisionType decision,
    int targetVersion
) {}
