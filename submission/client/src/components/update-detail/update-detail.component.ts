import { Component, ChangeDetectionStrategy, input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  EngagementUpdateDetails,
  ChangeSummary,
  ChangeType,
  Impact,
} from '../../models/engagement-update.models';

@Component({
  selector: 'app-update-detail',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './update-detail.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UpdateDetailComponent {
  readonly details = input.required<EngagementUpdateDetails>();
  readonly apply = output<void>();
  readonly decline = output<void>();

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
    this.apply.emit();
  }

  onDecline(): void {
    this.decline.emit();
  }

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
