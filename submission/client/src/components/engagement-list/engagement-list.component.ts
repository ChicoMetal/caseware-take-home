import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngagementUpdateService } from '../../services/engagement-update.service';
import { UpdateDetailComponent } from '../update-detail/update-detail.component';
import { UpdateStatus, DecisionType } from '../../models/engagement-update.models';

/**
 * Main list view showing all firm engagements and their template update status.
 * Delegates to {@link UpdateDetailComponent} for the selected engagement's detail panel.
 */
@Component({
  selector: 'app-engagement-list',
  standalone: true,
  imports: [CommonModule, UpdateDetailComponent],
  templateUrl: './engagement-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EngagementListComponent {
  private readonly updateService = inject(EngagementUpdateService);

  /** Exposed for template enum comparisons (e.g., status === UpdateStatus.PENDING). */
  readonly UpdateStatus = UpdateStatus;
  readonly engagements = this.updateService.engagements;
  readonly selectedDetails = this.updateService.selectedDetails;

  selectEngagement(engagementId: string): void {
    this.updateService.selectEngagement(engagementId);
  }

  onApply(engagementId: string): void {
    this.updateService.submitDecision(engagementId, DecisionType.APPLY);
  }

  onDecline(engagementId: string): void {
    this.updateService.submitDecision(engagementId, DecisionType.DECLINE);
  }

  /** Maps {@link UpdateStatus} enum values to user-facing display text. */
  statusLabel(status: UpdateStatus): string {
    switch (status) {
      case UpdateStatus.UP_TO_DATE:
        return 'Up to date';
      case UpdateStatus.PENDING:
        return 'Update available';
      case UpdateStatus.COMPUTING:
        return 'Preparing summary...';
      case UpdateStatus.DECLINED:
        return 'Declined';
      case UpdateStatus.ERROR:
        return 'Error';
    }
  }
}
