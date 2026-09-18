import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { EngagementUpdateFacade } from '../facades/engagement-update.facade';
import { EngagementUpdateStore } from '../store/engagement-update.store';
import { EngagementUpdateApi } from '../api/engagement-update.api';
import { EngagementUpdateEffects, RETRY_CONFIG, RetryConfig } from '../effects/engagement-update.effects';
import {
  EngagementUpdateSummary,
  EngagementUpdateDetails,
  UpdateDecisionResponse,
  UpdateStatus,
  DecisionType,
  DecisionStatus,
  ChangeType,
  Impact,
} from '../models/engagement-update.models';
import { of, throwError } from 'rxjs';

/** Zero-delay retry config for deterministic tests. */
const TEST_RETRY_CONFIG: RetryConfig = {
  count: 3,
  delayFn: () => of(0),
};

const MOCK_ENGAGEMENTS: EngagementUpdateSummary[] = [
  {
    engagementId: 'ENG-TEST-1',
    engagementName: 'Test Engagement',
    templateId: 'TPL-1',
    templateDisplayName: 'Test Template',
    currentVersion: 4,
    latestVersion: 5,
    status: UpdateStatus.PENDING,
    pendingUpdateCount: 1,
    summaryAvailable: true,
    lastCheckedAt: '2026-01-01T00:00:00Z',
    declinedVersion: null,
  },
];

const MOCK_DETAILS: EngagementUpdateDetails = {
  engagementId: 'ENG-TEST-1',
  currentVersion: 4,
  latestVersion: 5,
  collapsedSummary: {
    fromVersion: 4,
    toVersion: 5,
    publishedAt: '2026-01-01T00:00:00Z',
    sections: [
      {
        sectionPath: 'planning',
        sectionDisplayName: 'Planning',
        changes: [
          { type: ChangeType.MODIFIED, description: 'Test change', impact: Impact.LOW },
        ],
      },
    ],
    totalChanges: 1,
  },
  stepByStepSummaries: [],
  freshness: { computedAt: '2026-01-01T00:00:00Z', templatePublishedAt: '2026-01-01T00:00:00Z' },
};

describe('EngagementUpdateFacade', () => {
  let facade: EngagementUpdateFacade;
  let store: EngagementUpdateStore;
  let apiSpy: jasmine.SpyObj<EngagementUpdateApi>;

  beforeEach(() => {
    apiSpy = jasmine.createSpyObj('EngagementUpdateApi', [
      'getEngagements',
      'getEngagementDetails',
      'submitDecision',
    ]);

    TestBed.configureTestingModule({
      providers: [
        EngagementUpdateFacade,
        EngagementUpdateStore,
        EngagementUpdateEffects,
        { provide: EngagementUpdateApi, useValue: apiSpy },
        { provide: RETRY_CONFIG, useValue: TEST_RETRY_CONFIG },
      ],
    });

    facade = TestBed.inject(EngagementUpdateFacade);
    store = TestBed.inject(EngagementUpdateStore);
  });

  it('should load engagements into the store', fakeAsync(() => {
    apiSpy.getEngagements.and.returnValue(of(MOCK_ENGAGEMENTS));

    facade.loadEngagements();
    tick();

    expect(facade.engagements().length).toBe(1);
    expect(facade.engagements()[0].engagementId).toBe('ENG-TEST-1');
    expect(facade.loading()).toBe(false);
  }));

  it('should set error state when API fails after retries', fakeAsync(() => {
    apiSpy.getEngagements.and.returnValue(
      throwError(() => new Error('503 Service Unavailable'))
    );

    facade.loadEngagements();
    tick();

    expect(facade.error()).toContain('Failed to load engagements');
    expect(facade.loading()).toBe(false);
  }));

  it('should load details when selecting an engagement', fakeAsync(() => {
    apiSpy.getEngagementDetails.and.returnValue(of(MOCK_DETAILS));

    facade.selectEngagement('ENG-TEST-1');
    tick();

    const details = facade.selectedDetails();
    expect(details).toBeTruthy();
    expect(details!.engagementId).toBe('ENG-TEST-1');
    expect(facade.detailLoading()).toBe(false);
  }));

  it('should transition to UP_TO_DATE on APPLY decision', fakeAsync(() => {
    const response: UpdateDecisionResponse = {
      engagementId: 'ENG-TEST-1',
      decision: DecisionType.APPLY,
      previousVersion: 4,
      targetVersion: 5,
      status: DecisionStatus.PROCESSING,
    };
    apiSpy.submitDecision.and.returnValue(of(response));
    store.setEngagements(MOCK_ENGAGEMENTS);

    facade.submitDecision('ENG-TEST-1', DecisionType.APPLY, 5);
    tick();

    const updated: EngagementUpdateSummary | undefined = facade.engagements()
      .find((e: EngagementUpdateSummary) => e.engagementId === 'ENG-TEST-1');
    expect(updated).toBeDefined();
    if (!updated) return;

    expect(updated.status).toBe(UpdateStatus.UP_TO_DATE);
    expect(updated.currentVersion).toBe(5);
    expect(updated.pendingUpdateCount).toBe(0);
    expect(facade.selectedDetails()).toBeNull();
  }));

  it('should transition to DECLINED on DECLINE decision', fakeAsync(() => {
    const response: UpdateDecisionResponse = {
      engagementId: 'ENG-TEST-1',
      decision: DecisionType.DECLINE,
      previousVersion: 4,
      targetVersion: 5,
      status: DecisionStatus.ACCEPTED,
    };
    apiSpy.submitDecision.and.returnValue(of(response));
    store.setEngagements(MOCK_ENGAGEMENTS);

    facade.submitDecision('ENG-TEST-1', DecisionType.DECLINE, 5);
    tick();

    const updated: EngagementUpdateSummary | undefined = facade.engagements()
      .find((e: EngagementUpdateSummary) => e.engagementId === 'ENG-TEST-1');
    expect(updated).toBeDefined();
    if (!updated) return;

    expect(updated.status).toBe(UpdateStatus.DECLINED);
    expect(updated.declinedVersion).toBe(5);
    expect(facade.selectedDetails()).toBeNull();
  }));

  it('should recover on retry after transient failure', fakeAsync(() => {
    let callCount = 0;
    apiSpy.getEngagements.and.callFake(() => {
      callCount++;
      if (callCount <= 2) {
        return throwError(() => new Error('503 Service Unavailable'));
      }
      return of(MOCK_ENGAGEMENTS);
    });

    facade.loadEngagements();
    tick();

    expect(callCount).toBe(3);
    expect(facade.engagements().length).toBe(1);
    expect(facade.error()).toBeNull();
    expect(facade.loading()).toBe(false);
  }));
});
