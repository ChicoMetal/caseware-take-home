package domain.model;

/**
 * Incoming user decision to apply or decline a template update.
 *
 * @param targetVersion the template version this decision targets; serves as an optimistic
 *                      concurrency check to prevent acting on a stale view
 */
public record UpdateDecision(
    DecisionType decision,
    int targetVersion
) {}
