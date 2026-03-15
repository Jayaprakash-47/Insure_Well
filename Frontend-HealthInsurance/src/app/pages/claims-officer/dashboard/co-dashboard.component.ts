import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ClaimsOfficerDashboardResponse, ClaimResponse }
  from '../../../core/models/models';

@Component({
  selector: 'app-co-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule],  // ← added CommonModule
  templateUrl: './co-dashboard.component.html',
  styleUrl: './co-dashboard.component.css',
})
export class CODashboardComponent implements OnInit {
  dashboard: ClaimsOfficerDashboardResponse | null = null;
  loading = true;

  // ── NEW: claims data for alert banners ──
  myClaims: ClaimResponse[] = [];

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.api.getOfficerDashboard().subscribe({
      next: (data) => {
        this.dashboard = data;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });

    // ── NEW: Load claims for alert counts ──
    this.api.getOfficerAssignedClaims().subscribe({
      next: (claims) => {
        this.myClaims = claims;
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  // ── NEW: Claims awaiting review ──
  get claimsAwaitingReview(): number {
    return this.myClaims.filter(
      c => c.claimStatus === 'UNDER_REVIEW').length;
  }

  // ── NEW: Claims approved but not yet settled ──
  get claimsPendingSettlement(): number {
    return this.myClaims.filter(
      c => c.claimStatus === 'APPROVED'
        || c.claimStatus === 'PARTIALLY_APPROVED').length;
  }

  // ── NEW: Claims submitted but not yet under review ──
  get claimsInQueue(): number {
    return this.myClaims.filter(
      c => c.claimStatus === 'SUBMITTED').length;
  }

  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }
}
