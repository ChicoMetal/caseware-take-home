// Types mirroring the server API contract defined in DESIGN.md

export enum UpdateStatus {
  UP_TO_DATE = 'UP_TO_DATE',
  PENDING = 'PENDING',
  COMPUTING = 'COMPUTING',
  DECLINED = 'DECLINED',
  ERROR = 'ERROR',
}

export enum ChangeType {
  ADDED = 'ADDED',
  MODIFIED = 'MODIFIED',
  REMOVED = 'REMOVED',
}

export enum Impact {
  HIGH = 'HIGH',
  MEDIUM = 'MEDIUM',
  LOW = 'LOW',
}

export interface EngagementUpdateSummary {
  engagementId: string;
  engagementName: string;
  templateId: string;
  templateDisplayName: string;
  currentVersion: number;
  latestVersion: number;
  status: UpdateStatus;
  pendingUpdateCount: number;
  summaryAvailable: boolean;
  lastCheckedAt: string;
  declinedVersion: number | null;
}

export interface EngagementUpdateDetails {
  engagementId: string;
  currentVersion: number;
  latestVersion: number;
  collapsedSummary: ChangeSummary;
  stepByStepSummaries: ChangeSummary[];
  freshness: {
    computedAt: string;
    templatePublishedAt: string;
  };
}

export interface ChangeSummary {
  fromVersion: number;
  toVersion: number;
  publishedAt: string;
  sections: SectionChange[];
  totalChanges: number;
}

export interface SectionChange {
  sectionPath: string;
  sectionDisplayName: string;
  changes: HumanReadableChange[];
}

export interface HumanReadableChange {
  type: ChangeType;
  description: string;
  impact: Impact;
}

export enum DecisionType {
  APPLY = 'APPLY',
  DECLINE = 'DECLINE',
}

export enum DecisionStatus {
  ACCEPTED = 'ACCEPTED',
  PROCESSING = 'PROCESSING',
}

export interface UpdateDecisionRequest {
  decision: DecisionType;
  targetVersion: number;
}

export interface UpdateDecisionResponse {
  engagementId: string;
  decision: DecisionType;
  previousVersion: number;
  targetVersion: number;
  status: DecisionStatus;
}
