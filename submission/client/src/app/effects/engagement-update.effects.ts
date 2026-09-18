import { Injectable, InjectionToken, inject } from '@angular/core';
import { Observable, defer, retry, timer, finalize, catchError, EMPTY } from 'rxjs';
import { EngagementUpdateApi } from '../api/engagement-update.api';
import { EngagementUpdateStore } from '../store/engagement-update.store';
import {
  DecisionType,
  UpdateStatus,
  UpdateDecisionRequest,
} from '../models/engagement-update.models';

/** Retry configuration: count and delay factory. Injectable for testability. */
export interface RetryConfig {
  count: number;
  delayFn: (error: Error, retryCount: number) => Observable<unknown>;
}

const DEFAULT_RETRY: RetryConfig = {
  count: 3,
  delayFn: (error: Error, retryCount: number) => {
    const backoff = 1000 * Math.pow(2, retryCount - 1);
    console.warn(
      `[EngagementEffects] retry ${retryCount}/${3} in ${backoff}ms — ${error.message}`
    );
    return timer(backoff);
  },
};

export const RETRY_CONFIG = new InjectionToken<RetryConfig>('RETRY_CONFIG', {
  providedIn: 'root',
  factory: () => DEFAULT_RETRY,
});

/**
 * Side-effect orchestrator for engagement update operations.
 *
 * Each public method triggers an API call, pipes it through a retry
 * strategy with exponential backoff, and writes the result (or error)
 * into the Store. Components never call the API or Store directly —
 * they go through the Facade, which delegates here.
 */
@Injectable({ providedIn: 'root' })
export class EngagementUpdateEffects {
  private readonly api = inject(EngagementUpdateApi);
  private readonly store = inject(EngagementUpdateStore);
  private readonly retryConfig = inject(RETRY_CONFIG);

  loadEngagements(): void {
    this.store.setEngagements([]);
    this.store.setLoading(true);
    this.store.setError(null);

    defer(() => this.api.getEngagements())
      .pipe(
        retry({
          count: this.retryConfig.count,
          delay: this.retryConfig.delayFn,
        }),
        finalize(() => this.store.setLoading(false)),
        catchError((error: Error) => {
          this.store.setError(
            `Failed to load engagements after ${this.retryConfig.count} retries: ${error.message}`
          );
          return EMPTY;
        }),
      )
      .subscribe(engagements => this.store.setEngagements(engagements));
  }

  loadDetails(engagementId: string): void {
    this.store.setSelectedDetails(null);
    this.store.setDetailLoading(true);
    this.store.setError(null);

    defer(() => this.api.getEngagementDetails(engagementId))
      .pipe(
        retry({
          count: this.retryConfig.count,
          delay: this.retryConfig.delayFn,
        }),
        finalize(() => this.store.setDetailLoading(false)),
        catchError((error: Error) => {
          this.store.setError(
            `Failed to load details for ${engagementId}: ${error.message}`
          );
          return EMPTY;
        }),
      )
      .subscribe(details => this.store.setSelectedDetails(details));
  }

  submitDecision(engagementId: string, decision: DecisionType, targetVersion: number): void {
    this.store.setLoading(true);
    this.store.setError(null);

    const request: UpdateDecisionRequest = { decision, targetVersion };

    defer(() => this.api.submitDecision(engagementId, request))
      .pipe(
        retry({
          count: this.retryConfig.count,
          delay: this.retryConfig.delayFn,
        }),
        finalize(() => this.store.setLoading(false)),
        catchError((error: Error) => {
          this.store.setError(
            `Failed to submit decision for ${engagementId}: ${error.message}`
          );
          return EMPTY;
        }),
      )
      .subscribe(response => {
        if (response.decision === DecisionType.APPLY) {
          this.store.updateEngagement(engagementId, {
            currentVersion: response.targetVersion,
            status: UpdateStatus.UP_TO_DATE,
            pendingUpdateCount: 0,
            summaryAvailable: false,
            declinedVersion: null,
          });
        } else {
          this.store.updateEngagement(engagementId, {
            status: UpdateStatus.DECLINED,
            declinedVersion: response.targetVersion,
          });
        }
        this.store.setSelectedDetails(null);
      });
  }
}
