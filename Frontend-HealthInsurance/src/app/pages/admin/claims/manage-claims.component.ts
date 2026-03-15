import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { ClaimResponse } from '../../../core/models/models';

@Component({
  selector: 'app-manage-claims',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './manage-claims.component.html',
  styleUrl: './manage-claims.component.css',
})
export class ManageClaimsComponent implements OnInit {
  claims: ClaimResponse[] = [];
  loading = true;
  showModal = false;
  selectedClaim: ClaimResponse | null = null;
  action = '';
  approvedAmount = 0;
  rejectionReason = '';
  processing = false;

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.api.getAllClaims().subscribe({
      next: (d) => {
        this.claims = d;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  openAction(claim: ClaimResponse, action: string): void {
    this.selectedClaim = claim;
    this.action = action;
    this.approvedAmount = claim.claimAmount;
    this.rejectionReason = '';
    this.showModal = true;
    this.cdr.detectChanges();
  }

  submit(): void {
    if (!this.selectedClaim) return;
    this.processing = true;
    const req =
      this.action === 'APPROVED'
        ? { status: 'APPROVED', approvedAmount: this.approvedAmount }
        : { status: 'REJECTED', rejectionReason: this.rejectionReason };

    this.api.updateClaimStatus(this.selectedClaim.claimId, req).subscribe({
      next: () => {
        this.toast.success(`Claim ${this.action.toLowerCase()}`);
        this.showModal = false;
        this.processing = false;
        this.loadClaims();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.processing = false;
        this.toast.error(err.error?.message || 'Failed');
        this.cdr.detectChanges();
      },
    });
  }

  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }
}
