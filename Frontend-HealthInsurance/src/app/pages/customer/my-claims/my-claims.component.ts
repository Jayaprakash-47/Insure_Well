import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ClaimResponse, PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-my-claims',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './my-claims.component.html',
  styleUrl: './my-claims.component.css',
})
export class MyClaimsComponent implements OnInit {
  claims: ClaimResponse[] = [];
  policies: PolicyResponse[] = [];
  loading = true;
  expandedClaimId: number | null = null; // ← NEW: track which claim shows timeline

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.api.getMyClaims().subscribe({
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
    this.api.getMyPolicies().subscribe({
      next: (d) => {
        this.policies = d;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  // ── NEW: Toggle timeline for a claim ──
  toggleTimeline(claimId: number): void {
    this.expandedClaimId = this.expandedClaimId === claimId ? null : claimId;
  }

  // ── NEW: Build timeline steps from claim ──
  getTimelineSteps(claim: ClaimResponse): TimelineStep[] {
    const steps: TimelineStep[] = [
      {
        key: 'SUBMITTED',
        label: 'Claim Submitted',
        description: 'Your claim has been received',
        icon: 'upload_file',
        date: claim.createdAt,
      },
      {
        key: 'UNDER_REVIEW',
        label: 'Under Review',
        description: 'Claims officer is reviewing your documents',
        icon: 'manage_search',
        date: claim.reviewStartedAt || null,
      },
      {
        key: 'APPROVED',
        label: 'Decision Made',
        description: this.getDecisionLabel(claim),
        icon: this.getDecisionIcon(claim),
        date: claim.reviewedAt || null,
      },
      {
        key: 'SETTLED',
        label: 'Settlement',
        description: claim.settlementAmount
          ? '₹' + claim.settlementAmount.toLocaleString('en-IN') + ' credited'
          : 'Amount will be credited to your account',
        icon: 'payments',
        date: claim.settlementDate || null,
      },
    ];

    // Mark each step's state
    const statusOrder = ['SUBMITTED', 'UNDER_REVIEW', 'APPROVED', 'PARTIALLY_APPROVED', 'SETTLED'];
    const currentIndex = statusOrder.indexOf(claim.claimStatus);
    const isRejected = claim.claimStatus === 'REJECTED';

    return steps.map((step, i) => ({
      ...step,
      state:
        isRejected && i === 2
          ? 'rejected'
          : isRejected && i > 2
            ? 'skipped'
            : i < this.getStepIndex(claim.claimStatus)
              ? 'completed'
              : i === this.getStepIndex(claim.claimStatus)
                ? 'active'
                : 'pending',
    }));
  }

  private getStepIndex(status: string): number {
    const map: Record<string, number> = {
      SUBMITTED: 0,
      UNDER_REVIEW: 1,
      APPROVED: 2,
      PARTIALLY_APPROVED: 2,
      REJECTED: 2,
      SETTLED: 3,
    };
    return map[status] ?? 0;
  }

  private getDecisionLabel(claim: ClaimResponse): string {
    if (claim.claimStatus === 'APPROVED')
      return 'Approved: ₹' + (claim.approvedAmount || 0).toLocaleString('en-IN');
    if (claim.claimStatus === 'PARTIALLY_APPROVED')
      return 'Partially approved: ₹' + (claim.approvedAmount || 0).toLocaleString('en-IN');
    if (claim.claimStatus === 'REJECTED')
      return 'Rejected: ' + (claim.rejectionReason || 'See details');
    return 'Awaiting decision';
  }

  private getDecisionIcon(claim: ClaimResponse): string {
    if (claim.claimStatus === 'APPROVED') return 'check_circle';
    if (claim.claimStatus === 'PARTIALLY_APPROVED') return 'rule';
    if (claim.claimStatus === 'REJECTED') return 'cancel';
    return 'pending';
  }

  // ── existing methods unchanged ──
  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }

  getRemainingCoverage(policyId: number): number | null {
    const p = this.policies.find((p) => p.policyId === policyId);
    if (!p) return null;
    let remaining = p.remainingCoverage ?? p.coverageAmount;
    const pendingPayouts = this.claims
      .filter(
        (c) =>
          c.policyId === policyId &&
          (c.claimStatus === 'APPROVED' || c.claimStatus === 'PARTIALLY_APPROVED'),
      )
      .reduce((sum, c) => sum + (c.approvedAmount || 0), 0);
    return remaining - pendingPayouts;
  }

  getCoveragePercent(policyId: number): number {
    const p = this.policies.find((pol) => pol.policyId === policyId);
    if (!p || !p.coverageAmount) return 100;
    const remaining = Math.max(0, this.getRemainingCoverage(policyId) || 0);
    return Math.round((remaining / p.coverageAmount) * 100);
  }

  getStatusClass(s: string): string {
    const m: Record<string, string> = {
      SUBMITTED: 'badge-submitted',
      UNDER_REVIEW: 'badge-info',
      APPROVED: 'badge-approved',
      PARTIALLY_APPROVED: 'badge-info',
      REJECTED: 'badge-rejected',
      SETTLED: 'badge-success',
      DOCUMENT_PENDING: 'badge-pending',
    };
    return m[s] || 'badge-info';
  }
}

export interface TimelineStep {
  key: string;
  label: string;
  description: string;
  icon: string;
  date: any;
  state?: 'completed' | 'active' | 'pending' | 'rejected' | 'skipped';
}
