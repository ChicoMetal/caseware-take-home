import { Component, ChangeDetectionStrategy, inject, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EngagementUpdateFacade } from '../../facades/engagement-update.facade';
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
export class EngagementListComponent implements OnInit {
  private readonly facade = inject(EngagementUpdateFacade);

  readonly UpdateStatus = UpdateStatus;
  readonly engagements = this.facade.engagements;
  readonly selectedDetails = this.facade.selectedDetails;
  readonly loading = this.facade.loading;
  readonly detailLoading = this.facade.detailLoading;
  readonly error = this.facade.error;

  ngOnInit(): void {
    this.facade.loadEngagements();
  }

  selectEngagement(engagementId: string): void {
    this.facade.selectEngagement(engagementId);
  }

  onApply(engagementId: string, targetVersion: number): void {
    this.facade.submitDecision(engagementId, DecisionType.APPLY, targetVersion);
  }

  onDecline(engagementId: string, targetVersion: number): void {
    this.facade.submitDecision(engagementId, DecisionType.DECLINE, targetVersion);
  }

  closeModal(): void {
    this.facade.clearSelection();
  }

  onBackdropClick(event: MouseEvent): void {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.closeModal();
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    if (this.selectedDetails()) {
      this.closeModal();
    }
  }

  retry(): void {
    this.facade.clearError();
    this.facade.loadEngagements();
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
      default:
        return status satisfies never;
    }
  }
}
