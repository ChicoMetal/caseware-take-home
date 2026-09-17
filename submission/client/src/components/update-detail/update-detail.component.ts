import { Component, Input, Output, EventEmitter, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  EngagementUpdateDetails,
  ChangeSummary,
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

  changeTypeLabel(type: string): string {
    switch (type) {
      case 'ADDED':
        return '+';
      case 'MODIFIED':
        return '~';
      case 'REMOVED':
        return '-';
      default:
        return '?';
    }
  }

  impactLabel(impact: string): string {
    switch (impact) {
      case 'HIGH':
        return '[HIGH]';
      case 'MEDIUM':
        return '[MED]';
      case 'LOW':
        return '[LOW]';
      default:
        return '';
    }
  }
}
