package domain.model;

import java.util.Objects;

/**
 * Incoming user decision to apply or decline a template update.
 *
 * @param targetVersion the template version this decision targets; serves as an optimistic
 *                      concurrency check to prevent acting on a stale view
 */
public record UpdateDecision(
    DecisionType decision,
    int targetVersion
) {
    public UpdateDecision {
        Objects.requireNonNull(decision, "decision");
        if (targetVersion <= 0) {
            throw new IllegalArgumentException("targetVersion must be positive");
        }
    }
}
