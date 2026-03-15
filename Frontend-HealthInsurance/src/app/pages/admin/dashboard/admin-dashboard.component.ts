import { Component, OnInit, ChangeDetectorRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { DashboardResponse, PolicyResponse, ClaimResponse } from '../../../core/models/models';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
export class AdminDashboardComponent implements OnInit, AfterViewInit {
  stats: DashboardResponse | null = null;
  loading = true;
  recentPolicies: PolicyResponse[] = [];
  recentClaims: ClaimResponse[] = [];

  // ── NEW: Audit log ──
  auditLogs: any[] = [];
  auditLoading = false;
  selectedAuditRole = 'ALL';
  auditRoles = ['ALL', 'ADMIN', 'CUSTOMER', 'UNDERWRITER', 'CLAIMS_OFFICER', 'SYSTEM'];

  // ── NEW: Active tab ──
  activeTab: 'overview' | 'charts' | 'audit' = 'overview';

  // ── NEW: Chart data computed from stats ──
  policyChartBars: ChartBar[] = [];
  claimChartBars: ChartBar[] = [];
  chartsReady = false;

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.api.getAdminDashboard().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
        this.buildCharts();
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
    this.api.getAllPolicies().subscribe({
      next: (data) => {
        this.recentPolicies = data;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
    this.api.getAllClaims().subscribe({
      next: (data) => {
        this.recentClaims = data;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
    this.loadAuditLogs();
  }

  ngAfterViewInit(): void {}

  // ── NEW: Build chart bars from stats ──
  buildCharts(): void {
    if (!this.stats) return;

    const totalPolicies =
      (this.stats.totalPendingPolicies || 0) +
        (this.stats.totalAssignedPolicies || 0) +
        (this.stats.totalQuoteSentPolicies || 0) +
        (this.stats.totalActivePolicies || 0) +
        (this.stats.totalExpiredPolicies || 0) || 1;

    this.policyChartBars = [
      {
        label: 'Pending',
        value: this.stats.totalPendingPolicies || 0,
        color: '#f59e0b',
        pct: this.pct(this.stats.totalPendingPolicies, totalPolicies),
      },
      {
        label: 'Assigned',
        value: this.stats.totalAssignedPolicies || 0,
        color: '#3b82f6',
        pct: this.pct(this.stats.totalAssignedPolicies, totalPolicies),
      },
      {
        label: 'Quoted',
        value: this.stats.totalQuoteSentPolicies || 0,
        color: '#8b5cf6',
        pct: this.pct(this.stats.totalQuoteSentPolicies, totalPolicies),
      },
      {
        label: 'Active',
        value: this.stats.totalActivePolicies || 0,
        color: '#10b981',
        pct: this.pct(this.stats.totalActivePolicies, totalPolicies),
      },
      {
        label: 'Expired',
        value: this.stats.totalExpiredPolicies || 0,
        color: '#94a3b8',
        pct: this.pct(this.stats.totalExpiredPolicies, totalPolicies),
      },
    ];

    const totalClaims =
      (this.stats.totalPendingClaims || 0) +
        (this.stats.totalUnderReviewClaims || 0) +
        (this.stats.totalApprovedClaims || 0) +
        (this.stats.totalSettledClaims || 0) +
        (this.stats.totalRejectedClaims || 0) || 1;

    this.claimChartBars = [
      {
        label: 'Submitted',
        value: this.stats.totalPendingClaims || 0,
        color: '#f59e0b',
        pct: this.pct(this.stats.totalPendingClaims, totalClaims),
      },
      {
        label: 'Under Review',
        value: this.stats.totalUnderReviewClaims || 0,
        color: '#3b82f6',
        pct: this.pct(this.stats.totalUnderReviewClaims, totalClaims),
      },
      {
        label: 'Approved',
        value: this.stats.totalApprovedClaims || 0,
        color: '#10b981',
        pct: this.pct(this.stats.totalApprovedClaims, totalClaims),
      },
      {
        label: 'Settled',
        value: this.stats.totalSettledClaims || 0,
        color: '#16a34a',
        pct: this.pct(this.stats.totalSettledClaims, totalClaims),
      },
      {
        label: 'Rejected',
        value: this.stats.totalRejectedClaims || 0,
        color: '#ef4444',
        pct: this.pct(this.stats.totalRejectedClaims, totalClaims),
      },
    ];

    this.chartsReady = true;
  }

  private pct(val: number | undefined, total: number): number {
    return Math.round(((val || 0) / total) * 100);
  }

  // ── NEW: Audit log methods ──
  loadAuditLogs(): void {
    this.auditLoading = true;
    this.api.getAuditLogs().subscribe({
      next: (logs) => {
        this.auditLogs = logs;
        this.auditLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.auditLoading = false;
      },
    });
  }

  get filteredAuditLogs(): any[] {
    if (this.selectedAuditRole === 'ALL') return this.auditLogs;
    return this.auditLogs.filter((l) => l.performedBy === this.selectedAuditRole);
  }

  filterAudit(role: string): void {
    this.selectedAuditRole = role;
    if (role === 'ALL') {
      this.loadAuditLogs();
    } else {
      this.auditLoading = true;
      this.api.getAuditLogsByRole(role).subscribe({
        next: (logs) => {
          this.auditLogs = logs;
          this.auditLoading = false;
          this.cdr.detectChanges();
        },
        error: () => {
          this.auditLoading = false;
        },
      });
    }
  }

  getAuditIcon(action: string): string {
    const icons: Record<string, string> = {
      POLICY_APPLIED: 'description',
      POLICY_ACTIVATED: 'check_circle',
      POLICY_CANCELLED: 'cancel',
      POLICY_RENEWED: 'autorenew',
      POLICY_REAPPLIED: 'refresh',
      POLICY_EXPIRED: 'history',
      CLAIM_SUBMITTED: 'upload_file',
      CLAIM_APPROVED: 'thumb_up',
      CLAIM_REJECTED: 'thumb_down',
      CLAIM_SETTLED: 'payments',
      QUOTE_SENT: 'send',
      CONCERN_RAISED: 'warning',
    };
    return icons[action] || 'info';
  }

  getAuditColor(action: string): string {
    if (action.includes('ACTIVATED') || action.includes('APPROVED') || action.includes('SETTLED'))
      return '#16a34a';
    if (action.includes('CANCELLED') || action.includes('REJECTED') || action.includes('EXPIRED'))
      return '#dc2626';
    if (action.includes('CONCERN')) return '#f59e0b';
    return '#3b82f6';
  }

  getRoleBadgeColor(role: string): string {
    const map: Record<string, string> = {
      ADMIN: '#1e40af',
      CUSTOMER: '#7c3aed',
      UNDERWRITER: '#065f46',
      CLAIMS_OFFICER: '#92400e',
      SYSTEM: '#64748b',
    };
    return map[role] || '#64748b';
  }

  // ── existing methods unchanged ──
  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      ACTIVE: 'badge-active',
      PENDING: 'badge-pending',
      EXPIRED: 'badge-inactive',
      CANCELLED: 'badge-cancelled',
      SUBMITTED: 'badge-submitted',
      UNDER_REVIEW: 'badge-info',
      APPROVED: 'badge-approved',
      REJECTED: 'badge-rejected',
      SETTLED: 'badge-success',
      ASSIGNED: 'badge-info',
      QUOTE_SENT: 'badge-info',
    };
    return map[status] || 'badge-info';
  }
  getSettledPctNum(): number {
    if (!this.stats) return 0;
    const total =
      (this.stats.totalPendingClaims || 0) +
      (this.stats.totalUnderReviewClaims || 0) +
      (this.stats.totalApprovedClaims || 0) +
      (this.stats.totalSettledClaims || 0) +
      (this.stats.totalRejectedClaims || 0);
    if (!total) return 0;
    return Math.round(((this.stats.totalSettledClaims || 0) / total) * 100);
  }

  getSettledPct(): number {
    // SVG stroke-dashoffset: 251.2 = full circle, 0 = full fill
    return 251.2 - (251.2 * this.getSettledPctNum()) / 100;
  }
}

export interface ChartBar {
  label: string;
  value: number;
  color: string;
  pct: number;
}
