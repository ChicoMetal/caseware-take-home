/**
 * Types mirroring the server API contract defined in DESIGN.md.
 * These interfaces and enums are the canonical client-side representation
 * of the template update notification domain model.
 */

/**
 * Lifecycle state of an engagement's relationship to its template version.
 *
 * Transitions: UP_TO_DATE -> PENDING (when a new template version is published),
 * PENDING -> DECLINED (user declines), DECLINED -> PENDING (newer version published),
 * PENDING -> UP_TO_DATE (user applies). COMPUTING is a transient server-side state
 * while the change summary is being generated.
 */
export enum UpdateStatus {
  UP_TO_DATE = 'UP_TO_DATE',
  PENDING = 'PENDING',
  COMPUTING = 'COMPUTING',
  DECLINED = 'DECLINED',
  ERROR = 'ERROR',
}

/** Maps to JSON Patch operation categories (add, replace, remove). */
export enum ChangeType {
  ADDED = 'ADDED',
  MODIFIED = 'MODIFIED',
  REMOVED = 'REMOVED',
}

/** Business impact level used to prioritize review of individual changes. */
export enum Impact {
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
}

/**
 * List-level view of an engagement's template update state.
 * One entry per engagement visible to the current firm.
 */
export interface EngagementUpdateSummary {
  engagementId: string;
  engagementName: string;
  templateId: string;
  templateDisplayName: string;
  currentVersion: number;
  latestVersion: number;
  status: UpdateStatus;
  pendingUpdateCount: number;
  /** Whether the server has finished computing the change summary. */
  summaryAvailable: boolean;
  lastCheckedAt: string;
  /**
   * The template version the user previously declined, or null if no active decline.
   * Resets to null when a newer template version is published (re-entering PENDING).
   */
  declinedVersion: number | null;
}

/**
 * Detail view for a single engagement's pending update.
 * Provides both a collapsed (all changes merged) and per-version breakdown,
 * plus freshness metadata so the UI can indicate staleness.
 */
export interface EngagementUpdateDetails {
  engagementId: string;
  currentVersion: number;
  latestVersion: number;
  /** All changes across skipped versions merged into one summary. */
  collapsedSummary: ChangeSummary;
  /** One summary per intermediate version, for step-by-step review. */
  stepByStepSummaries: ChangeSummary[];
  freshness: {
    computedAt: string;
    templatePublishedAt: string;
  };
}

/** Aggregated changes for a single version transition (or a collapsed range). */
export interface ChangeSummary {
  fromVersion: number;
  toVersion: number;
  publishedAt: string;
  sections: SectionChange[];
  totalChanges: number;
}

/** Groups human-readable changes under a template section (e.g., "Planning"). */
export interface SectionChange {
  sectionPath: string;
  sectionDisplayName: string;
  changes: HumanReadableChange[];
}

/** A single change rendered for auditor review, with type and business impact. */
export interface HumanReadableChange {
  type: ChangeType;
  description: string;
  impact: Impact;
}

/** The two actions a user can take on a pending template update. */
export enum DecisionType {
  APPLY = 'APPLY',
  DECLINE = 'DECLINE',
}

/**
 * Server acknowledgment status. ACCEPTED means the decision was recorded
 * synchronously; PROCESSING means the apply is running asynchronously.
 */
export enum DecisionStatus {
  ACCEPTED = 'ACCEPTED',
  PROCESSING = 'PROCESSING',
}

/**
 * Payload sent when the user accepts or declines an update.
 * `targetVersion` enables optimistic concurrency -- the server rejects
 * the request if the template version has changed since the summary was loaded.
 */
export interface UpdateDecisionRequest {
  decision: DecisionType;
  targetVersion: number;
}

/** Server acknowledgment confirming the decision was recorded. */
export interface UpdateDecisionResponse {
  engagementId: string;
  decision: DecisionType;
  previousVersion: number;
  targetVersion: number;
  status: DecisionStatus;
}
