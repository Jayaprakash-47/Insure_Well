import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  PolicyResponse,
  ClaimResponse,
  PaymentResponse
} from '../../../core/models/models';

@Component({
  selector: 'app-customer-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './customer-dashboard.component.html',
  styleUrl: './customer-dashboard.component.css'
})
export class CustomerDashboardComponent implements OnInit {
  policies: PolicyResponse[] = [];
  claims: ClaimResponse[] = [];
  payments: PaymentResponse[] = [];
  loading = true;

  // ── NEW: Payment filters ──
  paymentFilter = 'ALL';    // ALL | policy number
  downloadingReceipt: number | null = null;

  constructor(
    private api: ApiService,
    public auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.api.getMyPolicies().subscribe({
      next: (d) => {
        this.policies = d;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); }
    });
    this.api.getMyClaims().subscribe({
      next: (d) => { this.claims = d; this.cdr.detectChanges(); },
      error: () => {}
    });
    // ── NEW: Load payments ──
    this.api.getMyPayments().subscribe({
      next: (d) => { this.payments = d; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  // ── existing getters unchanged ──
  get activePolicies(): number {
    return this.policies.filter(p => p.policyStatus === 'ACTIVE').length;
  }
  get pendingPolicies(): number {
    return this.policies.filter(p =>
      ['PENDING', 'ASSIGNED', 'QUOTE_SENT'].includes(p.policyStatus)).length;
  }
  get pendingClaims(): number {
    return this.claims.filter(c => c.claimStatus === 'SUBMITTED').length;
  }

  // ── existing coverage analytics ──
  get totalCoverage(): number {
    return this.policies
      .filter(p => p.policyStatus === 'ACTIVE')
      .reduce((sum, p) => sum + (p.coverageAmount || 0), 0);
  }
  get totalRemaining(): number {
    return this.policies
      .filter(p => p.policyStatus === 'ACTIVE')
      .reduce((sum, p) => {
        const coverage = p.coverageAmount || 0;
        const deducted = this.claims
          .filter(c => c.policyId === p.policyId &&
            ['APPROVED', 'PARTIALLY_APPROVED', 'SETTLED']
              .includes(c.claimStatus))
          .reduce((s, c) => s + (c.approvedAmount || 0), 0);
        return sum + Math.max(0, coverage - deducted);
      }, 0);
  }
  get totalClaimed(): number {
    return this.claims
      .filter(c => ['APPROVED', 'PARTIALLY_APPROVED', 'SETTLED']
        .includes(c.claimStatus))
      .reduce((s, c) => s + (c.approvedAmount || 0), 0);
  }
  get coverageUsedPct(): number {
    if (!this.totalCoverage) return 0;
    return Math.round((this.totalClaimed / this.totalCoverage) * 100);
  }
  get coverageRemainingPct(): number {
    return 100 - this.coverageUsedPct;
  }
  get claimsBreakdown(): any[] {
    const statuses = [
      { key: 'SUBMITTED',    label: 'Submitted',    color: '#f59e0b' },
      { key: 'UNDER_REVIEW', label: 'Under Review', color: '#3b82f6' },
      { key: 'APPROVED',     label: 'Approved',     color: '#10b981' },
      { key: 'SETTLED',      label: 'Settled',      color: '#16a34a' },
      { key: 'REJECTED',     label: 'Rejected',     color: '#ef4444' }
    ];
    const total = this.claims.length || 1;
    return statuses
      .map(s => ({
        ...s,
        count: this.claims.filter(c => c.claimStatus === s.key).length,
        pct: Math.round(
          (this.claims.filter(c => c.claimStatus === s.key).length
            / total) * 100)
      }))
      .filter(s => s.count > 0);
  }
  get expiringSoon(): PolicyResponse[] {
    return this.policies.filter(p => {
      if (p.policyStatus !== 'ACTIVE' || !p.endDate) return false;
      const days = Math.ceil(
        (new Date(p.endDate).getTime() - new Date().getTime())
        / (1000 * 60 * 60 * 24));
      return days <= 30 && days > 0;
    });
  }
  get ncbPolicies(): PolicyResponse[] {
    return this.policies.filter(p =>
      p.policyStatus === 'ACTIVE' && (p.noClaimBonus || 0) > 0);
  }
  getDaysUntilExpiry(policy: PolicyResponse): number {
    if (!policy.endDate) return 0;
    return Math.ceil(
      (new Date(policy.endDate).getTime() - new Date().getTime())
      / (1000 * 60 * 60 * 24));
  }

  // ── NEW: Payment history computed properties ──
  get filteredPayments(): PaymentResponse[] {
    if (this.paymentFilter === 'ALL') return this.payments;
    return this.payments.filter(
      p => p.policyNumber === this.paymentFilter);
  }

  get totalAmountPaid(): number {
    return this.payments
      .filter(p => p.paymentStatus === 'SUCCESS')
      .reduce((s, p) => s + (p.amount || 0), 0);
  }

  get uniquePolicyNumbers(): string[] {
    return [...new Set(this.payments.map(p => p.policyNumber))];
  }

  getPaymentMethodIcon(method: string): string {
    const icons: Record<string, string> = {
      RAZORPAY:    '💳',
      UPI:         '📱',
      CREDIT_CARD: '💳',
      DEBIT_CARD:  '💳',
      NET_BANKING: '🏦'
    };
    return icons[method] || '💰';
  }

  getPaymentStatusColor(status: string): string {
    return status === 'SUCCESS' ? '#16a34a'
      : status === 'FAILED'  ? '#dc2626'
        : '#f59e0b';
  }

  // ── NEW: Download receipt ──
  downloadReceipt(payment: PaymentResponse): void {
    this.downloadingReceipt = payment.paymentId;
    const token = localStorage.getItem('hs_token');
    const url = `${this.api['api']}/payments/${payment.paymentId}/receipt`;

    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then(res => {
        if (!res.ok) throw new Error('Failed');
        return res.blob();
      })
      .then(blob => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `receipt_${payment.transactionId}.pdf`;
        a.click();
        URL.revokeObjectURL(a.href);
        this.downloadingReceipt = null;
        this.cdr.detectChanges();
      })
      .catch(() => {
        this.downloadingReceipt = null;
        this.cdr.detectChanges();
      });
  }

  // ── Policy timeline methods ──
  getPolicySteps(policy: PolicyResponse): any[] {
    const steps = [
      { key: 'PENDING',    label: 'Applied',     icon: 'description' },
      { key: 'ASSIGNED',   label: 'Assigned',    icon: 'person_search' },
      { key: 'QUOTE_SENT', label: 'Quote Ready', icon: 'request_quote' },
      { key: 'ACTIVE',     label: 'Active',      icon: 'verified_user' }
    ];
    const orderMap: Record<string, number> = {
      PENDING: 0, ASSIGNED: 1, QUOTE_SENT: 2, ACTIVE: 3,
      EXPIRED: 3, CANCELLED: 3, RENEWED: 3, CONCERN_RAISED: 1
    };
    const currentIndex = orderMap[policy.policyStatus] ?? 0;
    return steps.map((step, i) => ({
      ...step,
      state: i < currentIndex ? 'completed'
        : i === currentIndex ? 'active'
          : 'pending'
    }));
  }

  getPolicyStatusColor(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: '#10b981', PENDING: '#f59e0b',
      ASSIGNED: '#3b82f6', QUOTE_SENT: '#8b5cf6',
      EXPIRED: '#94a3b8', CANCELLED: '#ef4444',
      CONCERN_RAISED: '#dc2626'
    };
    return map[status] || '#64748b';
  }

  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }
}
