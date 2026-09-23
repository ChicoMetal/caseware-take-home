import {
  EngagementUpdateSummary,
  EngagementUpdateDetails,
  UpdateStatus,
  ChangeType,
  Impact,
} from '../models/engagement-update.models';

/** Fixture detail data keyed by engagement ID, simulating GET /api/engagements/{id}/update-details. */
export const FIXTURE_DETAILS: Record<string, EngagementUpdateDetails> = {
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

/** Fixture engagement list, simulating GET /api/firms/FIRM-01/engagements/updates. */
export const FIXTURE_ENGAGEMENTS: EngagementUpdateSummary[] = [
  { engagementId: 'ENG-1001', firmId: 'FIRM-01', engagementName: 'Northstar Manufacturing 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 5, latestVersion: 5, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1002', firmId: 'FIRM-01', engagementName: 'Maple Ridge Foods 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 4, latestVersion: 5, status: UpdateStatus.PENDING, pendingUpdateCount: 1, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1003', firmId: 'FIRM-01', engagementName: 'Harbourview Logistics 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 3, latestVersion: 5, status: UpdateStatus.PENDING, pendingUpdateCount: 2, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1004', firmId: 'FIRM-01', engagementName: 'Pinecrest Holdings 2026', templateId: 'AUDIT-CA', templateDisplayName: 'Canadian Audit Engagement', currentVersion: 5, latestVersion: 5, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1005', firmId: 'FIRM-01', engagementName: 'Cedar Peak Services 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 8, latestVersion: 8, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1006', firmId: 'FIRM-01', engagementName: 'Westmount Consulting 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 7, latestVersion: 8, status: UpdateStatus.PENDING, pendingUpdateCount: 1, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1007', firmId: 'FIRM-01', engagementName: 'Bluewater Hospitality 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 6, latestVersion: 8, status: UpdateStatus.PENDING, pendingUpdateCount: 2, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1008', firmId: 'FIRM-01', engagementName: 'Summit Property Group 2026', templateId: 'REVIEW-CA', templateDisplayName: 'Canadian Review Engagement', currentVersion: 8, latestVersion: 8, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1009', firmId: 'FIRM-01', engagementName: 'Northern Grid Energy 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 12, latestVersion: 12, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1010', firmId: 'FIRM-01', engagementName: 'Greenfield Health Services 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 11, latestVersion: 12, status: UpdateStatus.PENDING, pendingUpdateCount: 1, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1011', firmId: 'FIRM-01', engagementName: 'Stonebridge Construction 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 10, latestVersion: 12, status: UpdateStatus.PENDING, pendingUpdateCount: 2, summaryAvailable: true, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
  { engagementId: 'ENG-1012', firmId: 'FIRM-01', engagementName: 'Prairie Star Investments 2026', templateId: 'RISK-CA', templateDisplayName: 'Canadian Risk Assessment', currentVersion: 12, latestVersion: 12, status: UpdateStatus.UP_TO_DATE, pendingUpdateCount: 0, summaryAvailable: false, lastCheckedAt: '2026-09-16T12:00:00Z', declinedVersion: null },
];
