package domain.model;

public record UpdateDecisionResponse(
    String engagementId,
    DecisionType decision,
    int previousVersion,
    int targetVersion,
    DecisionStatus status
) {}
