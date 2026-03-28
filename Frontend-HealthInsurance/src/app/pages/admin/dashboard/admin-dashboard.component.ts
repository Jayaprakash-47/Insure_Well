import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { DashboardResponse, PolicyResponse, ClaimResponse } from '../../../core/models/models';

export interface ChartBar { label: string; value: number; color: string; pct: number; }

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css',
})
export class AdminDashboardComponent implements OnInit {
  stats: DashboardResponse | null = null;
  loading = true;
  recentPolicies: PolicyResponse[] = [];
  recentClaims:   ClaimResponse[]  = [];

  activeTab: 'overview' | 'charts' | 'audit' = 'overview';
  activityTab: 'policies' | 'claims' = 'policies';

  animatedStats: Record<string, number> = {};
  policyChartBars: ChartBar[] = [];
  claimChartBars:  ChartBar[] = [];

  auditLogs: any[] = [];
  auditLoading = false;
  selectedAuditRole = 'ALL';
  auditSearch = '';
  auditRoles = ['ALL', 'ADMIN', 'CUSTOMER', 'UNDERWRITER', 'CLAIMS_OFFICER', 'SYSTEM'];
  underwriterPerformance: any[] = [];

  constructor(private api: ApiService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.api.getAdminDashboard().subscribe({
      next: (data) => {
        this.stats = data;
        this.loading = false;
        this.buildCharts();
        this.animateCounters();
        this.cdr.detectChanges();
      },
      error: () => { this.loading = false; this.cdr.detectChanges(); },
    });
    this.api.getAllPolicies().subscribe({
      next: (d) => { this.recentPolicies = d.slice(0, 6); this.cdr.detectChanges(); }, error: () => {},
    });
    this.api.getAllClaims().subscribe({
      next: (d) => { this.recentClaims = d.slice(0, 6); this.cdr.detectChanges(); }, error: () => {},
    });
    this.api.getUnderwriterPerformance().subscribe({
      next: (d) => { this.underwriterPerformance = d.slice(0, 5); this.cdr.detectChanges(); }, error: () => {},
    });
    this.loadAuditLogs();
  }

  animateCounters(): void {
    if (!this.stats) return;
    const targets: Record<string, number> = {
      totalCustomers:       +(this.stats.totalCustomers       || 0),
      totalActivePolicies:  +(this.stats.totalActivePolicies  || 0),
      totalPendingPolicies: +(this.stats.totalPendingPolicies || 0),
      totalPendingClaims:   +(this.stats.totalPendingClaims   || 0),
      totalSettledClaims:   +(this.stats.totalSettledClaims   || 0),
      totalUnderwriters:    +(this.stats.totalUnderwriters    || 0),
      totalRevenue:         +(this.stats.totalRevenue         || 0),
      totalClaimsPaidOut:   +(this.stats.totalClaimsPaidOut   || 0),
    };
    Object.keys(targets).forEach(k => this.animatedStats[k] = 0);
    const steps = 50; let step = 0;
    const timer = setInterval(() => {
      step++;
      const p = 1 - Math.pow(1 - step / steps, 3);
      Object.keys(targets).forEach(k => { this.animatedStats[k] = Math.round(targets[k] * p); });
      this.cdr.detectChanges();
      if (step >= steps) clearInterval(timer);
    }, 28);
  }

  buildCharts(): void {
    if (!this.stats) return;
    const tp = Math.max(1,
      (this.stats.totalPendingPolicies  || 0) + (this.stats.totalAssignedPolicies  || 0) +
      (this.stats.totalQuoteSentPolicies|| 0) + (this.stats.totalActivePolicies    || 0) +
      (this.stats.totalExpiredPolicies  || 0));
    this.policyChartBars = [
      { label: 'Pending',  value: this.stats.totalPendingPolicies   || 0, color: '#f59e0b', pct: this.pct(this.stats.totalPendingPolicies,   tp) },
      { label: 'Assigned', value: this.stats.totalAssignedPolicies  || 0, color: '#3b82f6', pct: this.pct(this.stats.totalAssignedPolicies,  tp) },
      { label: 'Quoted',   value: this.stats.totalQuoteSentPolicies || 0, color: '#8b5cf6', pct: this.pct(this.stats.totalQuoteSentPolicies, tp) },
      { label: 'Active',   value: this.stats.totalActivePolicies    || 0, color: '#10b981', pct: this.pct(this.stats.totalActivePolicies,    tp) },
      { label: 'Expired',  value: this.stats.totalExpiredPolicies   || 0, color: '#94a3b8', pct: this.pct(this.stats.totalExpiredPolicies,   tp) },
    ];
    const tc = Math.max(1,
      (this.stats.totalPendingClaims     || 0) + (this.stats.totalUnderReviewClaims || 0) +
      (this.stats.totalApprovedClaims    || 0) + (this.stats.totalSettledClaims     || 0) +
      (this.stats.totalRejectedClaims    || 0));
    this.claimChartBars = [
      { label: 'Submitted',    value: this.stats.totalPendingClaims     || 0, color: '#f59e0b', pct: this.pct(this.stats.totalPendingClaims,     tc) },
      { label: 'Under Review', value: this.stats.totalUnderReviewClaims || 0, color: '#3b82f6', pct: this.pct(this.stats.totalUnderReviewClaims, tc) },
      { label: 'Approved',     value: this.stats.totalApprovedClaims    || 0, color: '#10b981', pct: this.pct(this.stats.totalApprovedClaims,    tc) },
      { label: 'Settled',      value: this.stats.totalSettledClaims     || 0, color: '#16a34a', pct: this.pct(this.stats.totalSettledClaims,     tc) },
      { label: 'Rejected',     value: this.stats.totalRejectedClaims    || 0, color: '#ef4444', pct: this.pct(this.stats.totalRejectedClaims,    tc) },
    ];
  }
  private pct(v: number | undefined, t: number) { return Math.round(((v || 0) / t) * 100); }

  loadAuditLogs(): void {
    this.auditLoading = true;
    this.api.getAuditLogs().subscribe({
      next: (l) => { this.auditLogs = l; this.auditLoading = false; this.cdr.detectChanges(); },
      error: () => { this.auditLoading = false; },
    });
  }

  get filteredAuditLogs(): any[] {
    let logs = this.auditLogs;
    if (this.selectedAuditRole !== 'ALL') logs = logs.filter(l => l.performedBy === this.selectedAuditRole);
    if (this.auditSearch.trim()) {
      const q = this.auditSearch.toLowerCase();
      logs = logs.filter(l => l.action?.toLowerCase().includes(q) || l.details?.toLowerCase().includes(q) || l.userEmail?.toLowerCase().includes(q));
    }
    return logs;
  }

  filterAudit(role: string): void {
    this.selectedAuditRole = role;
    if (role === 'ALL') { this.loadAuditLogs(); return; }
    this.auditLoading = true;
    this.api.getAuditLogsByRole(role).subscribe({
      next: (l) => { this.auditLogs = l; this.auditLoading = false; this.cdr.detectChanges(); },
      error: () => { this.auditLoading = false; },
    });
  }

  getAuditIcon(action: string): string {
    const m: Record<string, string> = {
      POLICY_APPLIED:'description', POLICY_ACTIVATED:'check_circle', POLICY_CANCELLED:'cancel',
      POLICY_RENEWED:'autorenew', CLAIM_SUBMITTED:'upload_file', CLAIM_APPROVED:'thumb_up',
      CLAIM_REJECTED:'thumb_down', CLAIM_SETTLED:'payments', QUOTE_SENT:'send',
      CONCERN_RAISED:'warning', KYC_VERIFIED:'verified_user', KYC_REJECTED:'gpp_bad',
    };
    return m[action] || 'info';
  }
  getAuditColor(action: string): string {
    if (action?.includes('ACTIVATED')||action?.includes('APPROVED')||action?.includes('SETTLED')||action?.includes('VERIFIED')) return '#16a34a';
    if (action?.includes('CANCELLED')||action?.includes('REJECTED')||action?.includes('EXPIRED')) return '#dc2626';
    if (action?.includes('CONCERN')) return '#f59e0b';
    return '#3b82f6';
  }
  getRoleBadgeColor(role: string): string {
    const m: Record<string, string> = { ADMIN:'#1e40af', CUSTOMER:'#7c3aed', UNDERWRITER:'#065f46', CLAIMS_OFFICER:'#92400e', SYSTEM:'#64748b' };
    return m[role] || '#64748b';
  }
  get lossRatio(): number {
    const rev = +(this.stats?.totalRevenue||0), paid = +(this.stats?.totalClaimsPaidOut||0);
    return rev ? Math.min(100, Math.round((paid/rev)*100)) : 0;
  }
  get lossRatioColor(): string { return this.lossRatio < 40 ? '#10b981' : this.lossRatio < 70 ? '#f59e0b' : '#ef4444'; }
  getSettledPctNum(): number {
    if (!this.stats) return 0;
    const t = (this.stats.totalPendingClaims||0)+(this.stats.totalUnderReviewClaims||0)+(this.stats.totalApprovedClaims||0)+(this.stats.totalSettledClaims||0)+(this.stats.totalRejectedClaims||0);
    return t ? Math.round(((this.stats.totalSettledClaims||0)/t)*100) : 0;
  }
  formatCurrency(amount: number): string {
    if (!amount) return '₹0';
    if (amount >= 10000000) return '₹'+(amount/10000000).toFixed(1)+' Cr';
    if (amount >= 100000)   return '₹'+(amount/100000).toFixed(1)+' L';
    return '₹'+amount.toLocaleString('en-IN');
  }
  getStatusClass(status: string): string {
    const m: Record<string,string> = { ACTIVE:'badge-active', PENDING:'badge-pending', EXPIRED:'badge-inactive', CANCELLED:'badge-cancelled', SUBMITTED:'badge-submitted', UNDER_REVIEW:'badge-info', APPROVED:'badge-approved', REJECTED:'badge-rejected', SETTLED:'badge-success', ASSIGNED:'badge-info', QUOTE_SENT:'badge-info' };
    return m[status] || 'badge-info';
  }
  totalPoliciesCount(): number {
    if (!this.stats) return 0;
    return (this.stats.totalPendingPolicies||0)+(this.stats.totalAssignedPolicies||0)+(this.stats.totalQuoteSentPolicies||0)+(this.stats.totalActivePolicies||0)+(this.stats.totalExpiredPolicies||0);
  }
  totalClaimsCount(): number {
    if (!this.stats) return 0;
    return (this.stats.totalPendingClaims||0)+(this.stats.totalUnderReviewClaims||0)+(this.stats.totalApprovedClaims||0)+(this.stats.totalSettledClaims||0)+(this.stats.totalRejectedClaims||0);
  }
  getMaxCommission(): number {
    if (!this.underwriterPerformance.length) return 1;
    return Math.max(...this.underwriterPerformance.map(u => u.totalCommissionEarned||0)) || 1;
  }
  commissionBarPct(u: any): number { return Math.round(((u.totalCommissionEarned||0)/this.getMaxCommission())*100); }
}
