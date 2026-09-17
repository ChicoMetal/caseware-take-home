import { Component, ChangeDetectionStrategy, input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  EngagementUpdateDetails,
  ChangeSummary,
  ChangeType,
  Impact,
} from '../../models/engagement-update.models';

/**
 * Displays the change summary for a pending template update.
 *
 * Supports two viewing modes: collapsed (all changes across skipped versions
 * merged into one list) and step-by-step (one summary per intermediate version).
 * Emits apply/decline events for the parent to forward to the service.
 */
@Component({
  selector: 'app-update-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './update-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UpdateDetailComponent {
  /** The engagement update data to display. Required input from the parent list component. */
  readonly details = input.required<EngagementUpdateDetails>();

  /** Emitted when the user chooses to apply the pending template update. */
  readonly apply = output<void>();

  /** Emitted when the user chooses to decline the pending template update. */
  readonly decline = output<void>();

  /** Toggles between collapsed (false) and step-by-step (true) change views. */
  readonly showStepByStep = signal(false);

  /** Derives the collapsed (all-in-one) summary from the current detail input. */
  readonly activeSummary = computed<ChangeSummary>(
    () => this.details().collapsedSummary
  );

  /** Derives the per-version summaries from the current detail input. */
  readonly stepSummaries = computed<ChangeSummary[]>(
    () => this.details().stepByStepSummaries
  );

  toggleView(): void {
    this.showStepByStep.update((v) => !v);
  }

  onApply(): void {
    this.apply.emit();
  }

  onDecline(): void {
    this.decline.emit();
  }

  /** Maps {@link ChangeType} to a compact diff-style prefix (+, ~, -). */
  changeTypeLabel(type: ChangeType): string {
    switch (type) {
      case ChangeType.ADDED:
        return '+';
      case ChangeType.MODIFIED:
        return '~';
      case ChangeType.REMOVED:
        return '-';
    }
  }

  /** Maps {@link Impact} to a bracketed severity label for inline display. */
  impactLabel(impact: Impact): string {
    switch (impact) {
      case Impact.HIGH:
        return '[HIGH]';
      case Impact.MEDIUM:
        return '[MED]';
      case Impact.LOW:
        return '[LOW]';
    }
  }
}
