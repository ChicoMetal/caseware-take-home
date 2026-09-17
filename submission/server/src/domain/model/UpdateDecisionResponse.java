package domain.model;

public record UpdateDecisionResponse(
    String engagementId,
    String decision,
    int previousVersion,
    int targetVersion,
    String status
) {

    public static final String ACCEPTED = "ACCEPTED";
    public static final String PROCESSING = "PROCESSING";
}
