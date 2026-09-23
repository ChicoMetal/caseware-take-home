import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  EngagementUpdateSummary,
  EngagementUpdateDetails,
  UpdateDecisionRequest,
  UpdateDecisionResponse,
  DecisionType,
  DecisionStatus,
} from '../models/engagement-update.models';
import { FIXTURE_ENGAGEMENTS, FIXTURE_DETAILS } from '../data/engagement-update.fixtures';

const SIMULATED_DELAY_MS = 600;
const FAILURE_RATE = 0.3;

/**
 * Simulated HTTP client for the engagement update REST API.
 *
 * Returns Observables with artificial latency and a configurable failure rate
 * to exercise the retry/error-handling pipeline. In production this would
 * delegate to Angular's HttpClient against the endpoints defined in DESIGN.md.
 */
@Injectable({ providedIn: 'root' })
export class EngagementUpdateApi {

  /** GET /api/firms/{firmId}/engagements/updates */
  getEngagements(firmId: string): Observable<EngagementUpdateSummary[]> {
    return this.simulateRequest(
      () => structuredClone(FIXTURE_ENGAGEMENTS.filter(e => e.firmId === firmId))
    );
  }

  /** GET /api/firms/{firmId}/engagements/{id}/update-details */
  getEngagementDetails(engagementId: string): Observable<EngagementUpdateDetails> {
    return this.simulateRequest(() => {
      const details = FIXTURE_DETAILS[engagementId];
      if (!details) {
        throw new Error(`404 Not Found: engagement ${engagementId} has no detail data`);
      }
      return structuredClone(details);
    });
  }

  /** POST /api/firms/{firmId}/engagements/{id}/decision */
  submitDecision(
    engagementId: string,
    request: UpdateDecisionRequest
  ): Observable<UpdateDecisionResponse> {
    return this.simulateRequest(() => {
      const engagement = FIXTURE_ENGAGEMENTS.find(e => e.engagementId === engagementId);
      const previousVersion = engagement?.currentVersion ?? request.targetVersion - 1;
      const status = request.decision === DecisionType.APPLY
        ? DecisionStatus.PROCESSING
        : DecisionStatus.ACCEPTED;

      return {
        engagementId,
        decision: request.decision,
        previousVersion,
        targetVersion: request.targetVersion,
        status,
      };
    });
  }

  private simulateRequest<T>(factory: () => T): Observable<T> {
    return new Observable<T>(subscriber => {
      const delay = SIMULATED_DELAY_MS + Math.random() * 400;
      const timer = setTimeout(() => {
        try {
          if (Math.random() < FAILURE_RATE) {
            subscriber.error(new Error('503 Service Unavailable (simulated)'));
            return;
          }
          subscriber.next(factory());
          subscriber.complete();
        } catch (err) {
          subscriber.error(err);
        }
      }, delay);

      return () => clearTimeout(timer);
    });
  }
}
