package domain.port;

import domain.model.UpdateDecisionResponse;

/**
 * Port for persisting decision audit events.
 *
 * <p>Every apply/decline decision must be durably recorded for compliance
 * traceability. The infrastructure layer provides the concrete implementation
 * (database table, event bus, structured log sink).
 *
 * <p>The domain emits the event; it does not decide how or where it is stored.
 */
public interface DecisionAuditLog {

    void record(UpdateDecisionResponse decision);
}
