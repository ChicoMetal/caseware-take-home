import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngagementUpdateService } from '../../services/engagement-update.service';
import { UpdateDetailComponent } from '../update-detail/update-detail.component';

@Component({
  selector: 'app-engagement-list',
  standalone: true,
  imports: [CommonModule, UpdateDetailComponent],
  templateUrl: './engagement-list.component.html',
})
export class EngagementListComponent {
  private readonly updateService = inject(EngagementUpdateService);

  readonly engagements = this.updateService.engagements;
  readonly selectedDetails = this.updateService.selectedDetails;

  selectEngagement(engagementId: string): void {
    this.updateService.selectEngagement(engagementId);
  }

  onApply(engagementId: string): void {
    this.updateService.submitDecision(engagementId, 'APPLY');
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'UP_TO_DATE':
        return 'Up to date';
      case 'PENDING':
        return 'Update available';
      case 'COMPUTING':
        return 'Preparing summary...';
      case 'ERROR':
        return 'Error';
      default:
        return status;
    }
  }
}
