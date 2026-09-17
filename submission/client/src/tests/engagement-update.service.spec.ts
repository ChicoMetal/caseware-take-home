import { TestBed } from '@angular/core/testing';
import { EngagementUpdateService } from '../services/engagement-update.service';
import { UpdateStatus, DecisionType } from '../models/engagement-update.models';

describe('EngagementUpdateService', () => {
  let service: EngagementUpdateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EngagementUpdateService);
  });

  it('should transition engagement from PENDING to UP_TO_DATE on APPLY', () => {
    const before = service.engagements().find(
      (e) => e.engagementId === 'ENG-1002'
    );
    expect(before).toBeDefined();
    expect(before!.status).toBe(UpdateStatus.PENDING);
    expect(before!.currentVersion).toBe(4);

    service.submitDecision('ENG-1002', DecisionType.APPLY);

    const after = service.engagements().find(
      (e) => e.engagementId === 'ENG-1002'
    );
    expect(after).toBeDefined();
    expect(after!.status).toBe(UpdateStatus.UP_TO_DATE);
    expect(after!.currentVersion).toBe(5);
    expect(after!.pendingUpdateCount).toBe(0);
    expect(after!.summaryAvailable).toBe(false);

    expect(service.selectedDetails()).toBeNull();
  });
});
