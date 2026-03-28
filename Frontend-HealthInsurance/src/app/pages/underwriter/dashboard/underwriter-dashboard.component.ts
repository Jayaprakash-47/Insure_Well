// FILE: src/app/pages/underwriter/dashboard/underwriter-dashboard.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { UnderwriterDashboardResponse, PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-underwriter-dashboard',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './underwriter-dashboard.component.html',
  styleUrl: './underwriter-dashboard.component.css',
})
export class UnderwriterDashboardComponent implements OnInit {
  profile: UnderwriterDashboardResponse | null = null;
  policies: PolicyResponse[] = [];
  userName = '';

  activeTab: 'overview' | 'analytics' | 'assisted' = 'overview';

  // ── Assisted Applications ────────────────────────────────────────────
  pendingRequests: any[] = [];
  myAcceptedRequests: any[] = [];
  loadingRequests = false;
  acceptingId: number | null = null;

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit() {
    this.userName = this.auth.getUserName();

    this.api.getUnderwriterDashboard().subscribe({
      next: (data) => {
        this.profile = data;
        this.cdr.detectChanges();
      },
      error: (err) => console.error('Failed to load dashboard', err),
    });

    this.api.getUnderwriterPolicies().subscribe({
      next: (data) => {
        this.policies = data;
        this.cdr.detectChanges();
      },
      error: () => {},
    });

    this.loadAssistedRequests();
  }

  // ── Load all agent requests ──────────────────────────────────────────
  loadAssistedRequests(): void {
    this.loadingRequests = true;

    this.api.getPendingAgentRequests().subscribe({
      next: (data) => {
        this.pendingRequests = data;
        this.loadingRequests = false;
        this.cdr.detectChanges();
      },
      error: () => { this.loadingRequests = false; }
    });

    this.api.getMyAcceptedAgentRequests().subscribe({
      next: (data) => {
        this.myAcceptedRequests = data;
        this.cdr.detectChanges();
      },
      error: () => {}
    });
  }

  // ── How many pending requests (for tab badge) ─────────────────────────
  get pendingRequestCount(): number {
    return this.pendingRequests.length;
  }

  // ── Accept request and navigate to wizard ────────────────────────────
  acceptAndApply(req: any): void {
    this.acceptingId = req.id;
    this.api.acceptAgentRequest(req.id).subscribe({
      next: () => {
        this.acceptingId = null;
        this.toast.success(`Accepted request from ${req.customerName}`);
        // Navigate to apply wizard with requestId
        window.location.href =
          `/underwriter/apply-for-customer?requestId=${req.id}`;
      },
      error: (err) => {
        this.acceptingId = null;
        this.toast.error(err?.error?.message || 'Failed to accept request');
      }
    });
  }

  // ── Analytics getters (unchanged from original) ──────────────────────
  get policyStatusBars(): StatusBar[] {
    const statuses = [
      { key: 'ASSIGNED',   label: 'Assigned',   color: '#f59e0b' },
      { key: 'QUOTE_SENT', label: 'Quote Sent', color: '#8b5cf6' },
      { key: 'ACTIVE',     label: 'Active',     color: '#10b981' },
      { key: 'EXPIRED',    label: 'Expired',    color: '#94a3b8' },
      { key: 'CANCELLED',  label: 'Cancelled',  color: '#ef4444' },
    ];
    const total = this.policies.length || 1;
    return statuses
      .map((s) => ({
        ...s,
        count: this.policies.filter(
          (p) => p.policyStatus === s.key).length,
        pct: Math.round(
          (this.policies.filter(
            (p) => p.policyStatus === s.key).length / total) * 100,
        ),
      }))
      .filter((s) => s.count > 0);
  }

  get recentActivePolicies(): PolicyResponse[] {
    return this.policies
      .filter((p) => p.policyStatus === 'ACTIVE')
      .slice(0, 5);
  }

  get conversionRate(): number {
    const quoted = this.policies.filter(p =>
      ['QUOTE_SENT', 'ACTIVE', 'EXPIRED', 'RENEWED',
        'CANCELLED'].includes(p.policyStatus)).length;
    const converted = this.policies.filter(p =>
      ['ACTIVE', 'EXPIRED', 'RENEWED'].includes(p.policyStatus)).length;
    if (!quoted) return 0;
    return Math.round((converted / quoted) * 100);
  }

  get pendingQuoteCount(): number {
    return this.policies.filter(p => p.policyStatus === 'QUOTE_SENT').length;
  }

  get averagePremium(): number {
    const withPremium = this.policies.filter((p) => p.premiumAmount);
    if (!withPremium.length) return 0;
    return Math.round(
      withPremium.reduce(
        (s, p) => s + (p.premiumAmount || 0), 0) / withPremium.length,
    );
  }

  get topPlans(): PlanStat[] {
    const planMap: Record<string, number> = {};
    this.policies.forEach((p) => {
      if (p.planName) planMap[p.planName] = (planMap[p.planName] || 0) + 1;
    });
    return Object.entries(planMap)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 4);
  }

  formatAmount(val?: number): string {
    if (!val) return '₹0';
    if (val >= 100000) return '₹' + (val / 100000).toFixed(1) + 'L';
    if (val >= 1000)   return '₹' + (val / 1000).toFixed(1) + 'K';
    return '₹' + val;
  }

  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }
}

export interface StatusBar {
  key: string; label: string; color: string;
  count: number; pct: number;
}

export interface PlanStat {
  name: string; count: number;
}
