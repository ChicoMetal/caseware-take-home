package domain.model;

/** Outcome of processing a user's update decision: synchronous confirmation or async handoff. */
public enum DecisionStatus {
    /** Decision applied immediately (e.g. DECLINE). */
    ACCEPTED,
    /** Decision queued for asynchronous processing (e.g. APPLY requiring engagement reload). */
    PROCESSING
}
