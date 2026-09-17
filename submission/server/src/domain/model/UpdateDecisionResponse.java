package domain.model;

/** Server acknowledgment after processing a user's update decision. */
public record UpdateDecisionResponse(
    String engagementId,
    DecisionType decision,
    int previousVersion,
    int targetVersion,
    DecisionStatus status
) {}
