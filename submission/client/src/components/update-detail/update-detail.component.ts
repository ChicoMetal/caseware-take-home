import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
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
})
export class UpdateDetailComponent {
  @Input({ required: true }) details!: EngagementUpdateDetails;
  @Output() apply = new EventEmitter<void>();
  @Output() decline = new EventEmitter<void>();

  readonly showStepByStep = signal(false);

  get activeSummary(): ChangeSummary {
    return this.details.collapsedSummary;
  }

  get stepSummaries(): ChangeSummary[] {
    return this.details.stepByStepSummaries;
  }

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
