import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngagementUpdateService } from '../../services/engagement-update.service';
import { UpdateDetailComponent } from '../update-detail/update-detail.component';
import { UpdateStatus, DecisionType } from '../../models/engagement-update.models';

@Component({
  selector: 'app-engagement-list',
  standalone: true,
  imports: [CommonModule, UpdateDetailComponent],
  templateUrl: './engagement-list.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EngagementListComponent {
  private readonly updateService = inject(EngagementUpdateService);

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
