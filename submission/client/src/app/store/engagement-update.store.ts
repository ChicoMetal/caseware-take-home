import { Injectable, signal, computed } from '@angular/core';
import {
  EngagementUpdateSummary,
  EngagementUpdateDetails,
  UpdateStatus,
} from '../models/engagement-update.models';

/**
 * Signal-based state container for engagement update data.
 *
 * Holds all reactive state as Angular signals. Mutations are explicit
 * methods — no direct signal writes from outside. The store is the
 * single source of truth; Effects write to it, Facade reads from it.
 */
@Injectable({ providedIn: 'root' })
export class EngagementUpdateStore {

  // ── Primary state ──────────────────────────────────────────────

  readonly engagements = signal<EngagementUpdateSummary[]>([]);
  readonly selectedDetails = signal<EngagementUpdateDetails | null>(null);
  readonly loading = signal(false);
  readonly detailLoading = signal(false);
  readonly error = signal<string | null>(null);

  // ── Derived state ──────────────────────────────────────────────

  readonly pendingCount = computed(() =>
    this.engagements().filter(
      (e: EngagementUpdateSummary) => e.status === UpdateStatus.PENDING
    ).length
  );

  readonly hasError = computed(() => this.error() !== null);

  // ── Mutations ──────────────────────────────────────────────────

  setEngagements(engagements: EngagementUpdateSummary[]): void {
    this.engagements.set(engagements);
  }

  setSelectedDetails(details: EngagementUpdateDetails | null): void {
    this.selectedDetails.set(details);
  }

  setLoading(value: boolean): void {
    this.loading.set(value);
  }

  setDetailLoading(value: boolean): void {
    this.detailLoading.set(value);
  }

  setError(message: string | null): void {
    this.error.set(message);
  }

  updateEngagement(
    engagementId: string,
    changes: Partial<EngagementUpdateSummary>
  ): void {
    this.engagements.update((list: EngagementUpdateSummary[]) =>
      list.map((e: EngagementUpdateSummary) =>
        e.engagementId === engagementId ? { ...e, ...changes } : e
      )
    );
  }
}
