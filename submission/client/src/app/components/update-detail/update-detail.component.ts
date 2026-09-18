import { Component, ChangeDetectionStrategy, inject, input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngagementUpdateFacade } from '../../facades/engagement-update.facade';
import {
  EngagementUpdateDetails,
  ChangeSummary,
  ChangeType,
  Impact,
  DecisionType,
} from '../../models/engagement-update.models';

/**
 * Displays the change summary for a pending template update.
 *
 * Supports two viewing modes: collapsed (all changes across skipped versions
 * merged into one list) and step-by-step (one summary per intermediate version).
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

  readonly details = input.required<EngagementUpdateDetails>();

  readonly showStepByStep = signal(false);

  readonly activeSummary = computed<ChangeSummary>(
    () => this.details().collapsedSummary
  );

  readonly stepSummaries = computed<ChangeSummary[]>(
    () => this.details().stepByStepSummaries
  );

  toggleView(): void {
    this.showStepByStep.update((v) => !v);
  }

  onApply(): void {
    const d = this.details();
    this.facade.submitDecision(d.engagementId, DecisionType.APPLY, d.latestVersion);
  }

  onDecline(): void {
    const d = this.details();
    this.facade.submitDecision(d.engagementId, DecisionType.DECLINE, d.latestVersion);
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
