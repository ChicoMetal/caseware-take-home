import { Injectable, inject } from '@angular/core';
import { EngagementUpdateStore } from '../store/engagement-update.store';
import { EngagementUpdateEffects } from '../effects/engagement-update.effects';
import { DecisionType } from '../models/engagement-update.models';

/**
 * Public API for engagement update features.
 *
 * Components inject only this class. It exposes read-only signals from
 * the Store and delegates commands to Effects. This decouples the view
 * layer from the state management internals (Store, Effects, API).
 */
@Injectable({ providedIn: 'root' })
export class EngagementUpdateFacade {
  private readonly store = inject(EngagementUpdateStore);
  private readonly effects = inject(EngagementUpdateEffects);

  // ── Read-only state (components bind to these) ─────────────────

  readonly engagements = this.store.engagements.asReadonly();
  readonly selectedDetails = this.store.selectedDetails.asReadonly();
  readonly loading = this.store.loading.asReadonly();
  readonly detailLoading = this.store.detailLoading.asReadonly();
  readonly error = this.store.error.asReadonly();
  readonly pendingCount = this.store.pendingCount;

  // ── Commands ───────────────────────────────────────────────────

  loadEngagements(): void {
    this.effects.loadEngagements();
  }

  selectEngagement(engagementId: string): void {
    this.effects.loadDetails(engagementId);
  }

  submitDecision(engagementId: string, decision: DecisionType, targetVersion: number): void {
    this.effects.submitDecision(engagementId, decision, targetVersion);
  }

  clearSelection(): void {
    this.store.setSelectedDetails(null);
  }

  clearError(): void {
    this.store.setError(null);
  }
}
