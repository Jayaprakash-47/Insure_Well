import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
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

  // ── NEW: active tab ──
  activeTab: 'overview' | 'analytics' = 'overview';

  constructor(
    private api: ApiService,
    private auth: AuthService,
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
    // ── NEW: load policies for analytics ──
    this.api.getUnderwriterPolicies().subscribe({
      next: (data) => {
        this.policies = data;
        this.cdr.detectChanges();
      },
      error: () => {},
    });
  }

  // ── NEW: Policy status breakdown ──
  get policyStatusBars(): StatusBar[] {
    const statuses = [
      { key: 'ASSIGNED', label: 'Assigned', color: '#f59e0b' },
      { key: 'QUOTE_SENT', label: 'Quote Sent', color: '#8b5cf6' },
      { key: 'ACTIVE', label: 'Active', color: '#10b981' },
      { key: 'EXPIRED', label: 'Expired', color: '#94a3b8' },
      { key: 'CANCELLED', label: 'Cancelled', color: '#ef4444' },
    ];
    const total = this.policies.length || 1;
    return statuses
      .map((s) => ({
        ...s,
        count: this.policies.filter((p) => p.policyStatus === s.key).length,
        pct: Math.round(
          (this.policies.filter((p) => p.policyStatus === s.key).length / total) * 100,
        ),
      }))
      .filter((s) => s.count > 0);
  }

  // ── NEW: Monthly premium (last 6 months simulated from data) ──
  get recentActivePolicies(): PolicyResponse[] {
    return this.policies.filter((p) => p.policyStatus === 'ACTIVE').slice(0, 5);
  }

  // ── NEW: Conversion rate (active / total quoted) ──
  get conversionRate(): number {
    const quoted = this.policies.filter(p =>
      ['QUOTE_SENT', 'ACTIVE', 'EXPIRED', 'RENEWED',
        'CANCELLED'].includes(p.policyStatus)).length;
    const converted = this.policies.filter(p =>
      ['ACTIVE', 'EXPIRED', 'RENEWED'].includes(p.policyStatus)).length;
    if (!quoted) return 0;
    return Math.round((converted / quoted) * 100);
  }

// Add this getter to show pending quotes
  get pendingQuoteCount(): number {
    return this.policies.filter(p =>
      p.policyStatus === 'QUOTE_SENT').length;
  }

  // ── NEW: Average premium ──
  get averagePremium(): number {
    const withPremium = this.policies.filter((p) => p.premiumAmount);
    if (!withPremium.length) return 0;
    return Math.round(
      withPremium.reduce((s, p) => s + (p.premiumAmount || 0), 0) / withPremium.length,
    );
  }

  // ── NEW: Top 3 plans by count ──
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
    if (val >= 1000) return '₹' + (val / 1000).toFixed(1) + 'K';
    return '₹' + val;
  }

  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }
}

export interface StatusBar {
  key: string;
  label: string;
  color: string;
  count: number;
  pct: number;
}

export interface PlanStat {
  name: string;
  count: number;
}
