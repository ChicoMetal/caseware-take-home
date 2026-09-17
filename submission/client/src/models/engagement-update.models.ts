// Types mirroring the server API contract defined in DESIGN.md

export type UpdateStatus = 'UP_TO_DATE' | 'PENDING' | 'COMPUTING' | 'ERROR';
export type ChangeType = 'ADDED' | 'MODIFIED' | 'REMOVED';
export type Impact = 'HIGH' | 'MEDIUM' | 'LOW';

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

export interface UpdateDecisionRequest {
  decision: 'APPLY' | 'DECLINE';
  targetVersion: number;
}

export interface UpdateDecisionResponse {
  engagementId: string;
  decision: 'APPLY' | 'DECLINE';
  previousVersion: number;
  targetVersion: number;
  status: 'ACCEPTED' | 'PROCESSING';
}
