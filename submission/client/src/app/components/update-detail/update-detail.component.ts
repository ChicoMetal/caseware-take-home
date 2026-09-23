import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngagementUpdateFacade } from '../../facades/engagement-update.facade';
import {
  ChangeSummary,
  ChangeType,
  Impact,
  DecisionType,
} from '../../models/engagement-update.models';

/**
 * Displays the change summary for a pending template update.
 *
 * Reads selected engagement details directly from the Store via the Facade.
 * Supports collapsed and step-by-step viewing modes.
 * Handles apply/decline decisions directly via the Facade.
 */
@Component({
  selector: 'app-update-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './update-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UpdateDetailComponent {
  private readonly facade = inject(EngagementUpdateFacade);

  readonly loading = this.facade.detailLoading;
  readonly details = this.facade.selectedDetails;

  readonly showStepByStep = signal(false);

  readonly activeSummary = computed<ChangeSummary>(
    () => this.details()!.collapsedSummary
  );

  readonly stepSummaries = computed<ChangeSummary[]>(
    () => this.details()!.stepByStepSummaries
  );

  toggleView(): void {
    this.showStepByStep.update((v) => !v);
  }

  onApply(): void {
    this.facade.submitDecision(this.facade.activeFirmId(), this.details()!.engagementId, DecisionType.APPLY, this.details()!.latestVersion);
  }

  onDecline(): void {
    this.facade.submitDecision(this.facade.activeFirmId(), this.details()!.engagementId, DecisionType.DECLINE, this.details()!.latestVersion);
  }

  changeTypeLabel(type: ChangeType): string {
    switch (type) {
      case ChangeType.ADDED:
        return '+';
      case ChangeType.MODIFIED:
        return '~';
      case ChangeType.REMOVED:
        return '-';
      default:
        return type satisfies never;
    }
  }

  impactLabel(impact: Impact): string {
    switch (impact) {
      case Impact.HIGH:
        return '[HIGH]';
      case Impact.MEDIUM:
        return '[MED]';
      case Impact.LOW:
        return '[LOW]';
      default:
        return impact satisfies never;
    }
  }
}
