package domain.model;

public record UpdateDecision(
    String decision,
    int targetVersion
) {
    public static final String APPLY = "APPLY";
    public static final String DECLINE = "DECLINE";
}
