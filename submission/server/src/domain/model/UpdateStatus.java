package domain.model;

/**
 * Lifecycle state of an engagement's template update.
 * <ul>
 *   <li>{@code UP_TO_DATE} - engagement is on the latest template version</li>
 *   <li>{@code PENDING} - newer template version(s) available, awaiting user action</li>
 *   <li>{@code COMPUTING} - diff computation in progress (async)</li>
 *   <li>{@code DECLINED} - user explicitly declined the latest available version</li>
 *   <li>{@code ERROR} - diff computation or update failed</li>
 * </ul>
 * Transitions: UP_TO_DATE -> PENDING (new version published), PENDING -> DECLINED | COMPUTING,
 * COMPUTING -> UP_TO_DATE | ERROR, DECLINED -> PENDING (newer version published).
 */
public enum UpdateStatus {
    UP_TO_DATE,
    PENDING,
    COMPUTING,
    DECLINED,
    ERROR
}
