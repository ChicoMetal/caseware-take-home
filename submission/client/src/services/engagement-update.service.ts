import { Injectable, signal, computed } from '@angular/core';
import {
  EngagementUpdateSummary,
  EngagementUpdateDetails,
  UpdateStatus,
  ChangeType,
  Impact,
  DecisionType,
} from '../models/engagement-update.models';

// Fixture data built from the provided sample files

const FIXTURE_DETAILS: Record<string, EngagementUpdateDetails> = {
  'ENG-1002': {
    engagementId: 'ENG-1002',
    currentVersion: 4,
    latestVersion: 5,
    collapsedSummary: {
      fromVersion: 4,
      toVersion: 5,
      publishedAt: '2026-08-18T13:04:41Z',
      sections: [
        {
          sectionPath: 'planning',
          sectionDisplayName: 'Planning',
          changes: [
            {
              type: ChangeType.MODIFIED,
              description:
                "Question text updated from 'Has management identified significant estimates?' to 'Has management identified significant accounting estimates and related estimation uncertainty?'",
              impact: Impact.LOW,
            },
          ],
        },
        {
          sectionPath: 'materiality',
          sectionDisplayName: 'Materiality',
          changes: [
            {
              type: ChangeType.MODIFIED,
              description: 'Threshold percent changed from 4.5 to 4.0',
              impact: Impact.HIGH,
            },
          ],
        },
        {
          sectionPath: 'completion',
          sectionDisplayName: 'Completion',
          changes: [
            {
              type: ChangeType.ADDED,
              description: "New checklist: 'Subsequent events review'",
              impact: Impact.MEDIUM,
            },
          ],
        },
      ],
      totalChanges: 3,
    },
    stepByStepSummaries: [
      {
        fromVersion: 4,
        toVersion: 5,
        publishedAt: '2026-08-18T13:04:41Z',
        sections: [
          {
            sectionPath: 'planning',
            sectionDisplayName: 'Planning',
            changes: [
              {
                type: ChangeType.MODIFIED,
                description: 'Question text about significant estimates updated',
                impact: Impact.LOW,
              },
            ],
          },
          {
            sectionPath: 'materiality',
            sectionDisplayName: 'Materiality',
            changes: [
              {
                type: ChangeType.MODIFIED,
                description: 'Threshold percent changed from 4.5 to 4.0',
                impact: Impact.HIGH,
              },
            ],
          },
          {
            sectionPath: 'completion',
            sectionDisplayName: 'Completion',
            changes: [
              {
                type: ChangeType.ADDED,
                description: "New checklist: 'Subsequent events review'",
                impact: Impact.MEDIUM,
              },
            ],
          },
        ],
        totalChanges: 3,
      },
    ],
    freshness: {
      computedAt: '2026-08-18T13:05:00Z',
      templatePublishedAt: '2026-08-18T13:04:41Z',
    },
  },
  'ENG-1003': {
    engagementId: 'ENG-1003',
    currentVersion: 3,
    latestVersion: 5,
    collapsedSummary: {
      fromVersion: 3,
      toVersion: 5,
      publishedAt: '2026-08-18T13:04:41Z',
      sections: [
        {
          sectionPath: 'planning',
          sectionDisplayName: 'Planning',
          changes: [
            {
              type: ChangeType.ADDED,
              description:
                "New question: 'Were any new fraud risk factors identified during planning?' (required)",
              impact: Impact.HIGH,
            },
            {
              type: ChangeType.MODIFIED,
              description:
                'Question about significant estimates text expanded',
              impact: Impact.LOW,
            },
            {
              type: ChangeType.REMOVED,
              description:
                "Removed procedure: 'Confirm legacy risk classification'",
              impact: Impact.MEDIUM,
            },
          ],
        },
        {
          sectionPath: 'materiality',
          sectionDisplayName: 'Materiality',
          changes: [
            {
              type: ChangeType.MODIFIED,
              description: 'Threshold percent changed from 5.0 to 4.0',
              impact: Impact.HIGH,
            },
          ],
        },
        {
          sectionPath: 'completion',
          sectionDisplayName: 'Completion',
          changes: [
            {
              type: ChangeType.ADDED,
              description: "New checklist: 'Subsequent events review'",
              impact: Impact.MEDIUM,
            },
          ],
        },
      ],
      totalChanges: 5,
    },
    stepByStepSummaries: [
      {
        fromVersion: 3,
        toVersion: 4,
        publishedAt: '2026-07-07T13:02:18Z',
        sections: [
          {
            sectionPath: 'planning',
            sectionDisplayName: 'Planning',
            changes: [
              {
                type: ChangeType.ADDED,
                description:
                  "New question: 'Were any new fraud risk factors identified during planning?' (required)",
                impact: Impact.HIGH,
              },
              {
                type: ChangeType.REMOVED,
                description:
                  "Removed procedure: 'Confirm legacy risk classification'",
                impact: Impact.MEDIUM,
              },
            ],
          },
          {
            sectionPath: 'materiality',
            sectionDisplayName: 'Materiality',
            changes: [
              {
                type: ChangeType.MODIFIED,
                description: 'Threshold percent changed from 5.0 to 4.5',
                impact: Impact.HIGH,
              },
            ],
          },
        ],
        totalChanges: 3,
      },
      {
        fromVersion: 4,
        toVersion: 5,
        publishedAt: '2026-08-18T13:04:41Z',
        sections: [
          {
            sectionPath: 'planning',
            sectionDisplayName: 'Planning',
            changes: [
              {
                type: ChangeType.MODIFIED,
                description: 'Question text about significant estimates updated',
                impact: Impact.LOW,
              },
            ],
          },
          {
            sectionPath: 'materiality',
            sectionDisplayName: 'Materiality',
            changes: [
              {
                type: ChangeType.MODIFIED,
                description: 'Threshold percent changed from 4.5 to 4.0',
                impact: Impact.HIGH,
              },
            ],
          },
          {
            sectionPath: 'completion',
            sectionDisplayName: 'Completion',
            changes: [
              {
                type: ChangeType.ADDED,
                description: "New checklist: 'Subsequent events review'",
                impact: Impact.MEDIUM,
              },
            ],
          },
        ],
        totalChanges: 3,
      },
    ],
    freshness: {
      computedAt: '2026-08-18T13:05:00Z',
      templatePublishedAt: '2026-08-18T13:04:41Z',
    },
  },
};

const FIXTURE_ENGAGEMENTS: EngagementUpdateSummary[] = [
  { engagementId: 'ENG-1001', engagementName: 'Northstar Manufacturing 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 5, latestVersion: 5, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1002', engagementName: 'Maple Ridge Foods 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 4, latestVersion: 5, status: UpdateStatus.PENDING, pendingUpdateCount: 1, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1003', engagementName: 'Harbourview Logistics 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 3, latestVersion: 5, status: UpdateStatus.PENDING, pendingUpdateCount: 2, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1004', engagementName: 'Pinecrest Holdings 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 5, latestVersion: 5, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1005', engagementName: 'Cedar Peak Services 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 8, latestVersion: 8, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1006', engagementName: 'Westmount Consulting 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 7, latestVersion: 8, status: UpdateStatus.PENDING, pendingUpdateCount: 1, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1007', engagementName: 'Bluewater Hospitality 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 6, latestVersion: 8, status: UpdateStatus.PENDING, pendingUpdateCount: 2, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1008', engagementName: 'Summit Property Group 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 8, latestVersion: 8, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1009', engagementName: 'Northern Grid Energy 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 12, latestVersion: 12, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1010', engagementName: 'Greenfield Health Services 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 11, latestVersion: 12, status: UpdateStatus.PENDING, pendingUpdateCount: 1, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1011', engagementName: 'Stonebridge Construction 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 10, latestVersion: 12, status: UpdateStatus.PENDING, pendingUpdateCount: 2, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1012', engagementName: 'Prairie Star Investments 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 12, latestVersion: 12, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
];

@Injectable({ providedIn: 'root' })
export class EngagementUpdateService {
  readonly engagements = signal<EngagementUpdateSummary[]>(FIXTURE_ENGAGEMENTS);
  readonly selectedDetails = signal<EngagementUpdateDetails | null>(null);
  readonly loading = signal<boolean>(false);

  readonly pendingEngagements = computed(() =>
    this.engagements().filter((e) => e.status === UpdateStatus.PENDING)
  );

  selectEngagement(engagementId: string): void {
    this.loading.set(true);
    const details = FIXTURE_DETAILS[engagementId] ?? null;
    this.selectedDetails.set(details);
    this.loading.set(false);
  }

  clearSelection(): void {
    this.selectedDetails.set(null);
  }

  submitDecision(engagementId: string, decision: DecisionType): void {
    if (decision === DecisionType.APPLY) {
      this.engagements.update((engagements) =>
        engagements.map((e) => {
          if (e.engagementId !== engagementId) return e;
          return {
            ...e,
            currentVersion: e.latestVersion,
            status: UpdateStatus.UP_TO_DATE,
            pendingUpdateCount: 0,
            summaryAvailable: false,
            declinedVersion: null,
          };
        })
      );
    }

    if (decision === DecisionType.DECLINE) {
      this.engagements.update((engagements) =>
        engagements.map((e) => {
          if (e.engagementId !== engagementId) return e;
          return {
            ...e,
            status: UpdateStatus.DECLINED,
            declinedVersion: e.latestVersion,
          };
        })
      );
    }

    this.selectedDetails.set(null);
  }
}
