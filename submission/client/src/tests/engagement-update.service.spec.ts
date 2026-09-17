import { TestBed } from '@angular/core/testing';
import { EngagementUpdateService } from '../services/engagement-update.service';

describe('EngagementUpdateService', () => {
  let service: EngagementUpdateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(EngagementUpdateService);
  });

  it('should transition engagement from PENDING to UP_TO_DATE on APPLY', () => {
    // ENG-1002 starts as PENDING at v4, latest v5
    const before = service.engagements().find(
      (e) => e.engagementId === 'ENG-1002'
    );
    expect(before).toBeDefined();
    expect(before!.status).toBe('PENDING');
    expect(before!.currentVersion).toBe(4);

    // Apply the update
    service.submitDecision('ENG-1002', 'APPLY');

    // Verify state transition
    const after = service.engagements().find(
      (e) => e.engagementId === 'ENG-1002'
    );
    expect(after).toBeDefined();
    expect(after!.status).toBe('UP_TO_DATE');
    expect(after!.currentVersion).toBe(5);
    expect(after!.pendingUpdateCount).toBe(0);
    expect(after!.summaryAvailable).toBe(false);

    // Selection should be cleared after applying
    expect(service.selectedDetails()).toBeNull();
  });
});
