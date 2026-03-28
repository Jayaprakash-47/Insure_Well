// FILE: src/app/pages/underwriter/pending/underwriter-pending.component.ts

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-underwriter-pending',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './underwriter-pending.component.html',
  styleUrl: './underwriter-pending.component.css',
})
export class UnderwriterPendingComponent implements OnInit {
  policies: PolicyResponse[] = [];
  loading = true;

  quoteAmounts:    Record<number, number>  = {};
  quoteBreakdowns: Record<number, any[]>   = {};
  sending:         Record<number, boolean> = {};
  errors:          Record<number, string>  = {};
  successPolicyId: number | null = null;

  showConcernForm: Record<number, boolean> = {};
  concernRemarks:  Record<number, string>  = {};
  sendingConcern:  Record<number, boolean> = {};

  // Document loading state
  docLoading: Record<string, boolean> = {};

  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
    private toast: ToastService,
  ) {}

  ngOnInit(): void {
    this.api.getUnderwriterPendingAssignments().subscribe({
      next: (data) => {
        this.policies = data;
        this.loading  = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  // ── Documents ─────────────────────────────────────────────────────────

  viewHealthReport(policyId: number): void {
    const key = `${policyId}_report`;
    if (this.docLoading[key]) return;
    this.docLoading[key] = true;
    this.cdr.detectChanges();

    this.api.getPolicyDocument(policyId).subscribe({
      next: (blob) => {
        this.docLoading[key] = false;
        this.openInTab(blob, `health-report-${policyId}`);
        this.cdr.detectChanges();
      },
      error: () => {
        this.docLoading[key] = false;
        this.toast.error('No health report available for this policy');
        this.cdr.detectChanges();
      },
    });
  }

  private openInTab(blob: Blob, filename: string): void {
    const mimeType =
      blob.type && blob.type !== 'application/octet-stream' && blob.type !== ''
        ? blob.type
        : 'application/pdf';

    const typedBlob = new Blob([blob], { type: mimeType });
    const url = window.URL.createObjectURL(typedBlob);
    const win = window.open(url, '_blank');

    if (!win || win.closed) {
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
    }

    setTimeout(() => window.URL.revokeObjectURL(url), 30_000);
  }

  // ── Concern ───────────────────────────────────────────────────────────

  toggleConcernForm(policyId: number): void {
    this.showConcernForm[policyId] = !this.showConcernForm[policyId];
    if (!this.concernRemarks[policyId]) this.concernRemarks[policyId] = '';
  }

  submitConcern(policyId: number): void {
    const remarks = this.concernRemarks[policyId]?.trim();
    if (!remarks) {
      this.toast.error('Please provide concern details');
      return;
    }

    this.sendingConcern[policyId] = true;
    this.api.raiseConcern(policyId, remarks).subscribe({
      next: () => {
        this.toast.success('Concern raised — customer notified');
        this.showConcernForm[policyId] = false;
        this.sendingConcern[policyId]  = false;
        this.policies = this.policies.filter(p => p.policyId !== policyId);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.sendingConcern[policyId] = false;
        this.toast.error(err?.error?.message || 'Failed to raise concern');
        this.cdr.detectChanges();
      },
    });
  }

  // ── Quote ─────────────────────────────────────────────────────────────

  calculateQuote(policyId: number): void {
    this.sending[policyId] = true;
    this.errors[policyId]  = '';
    this.api.calculateUnderwriterQuote(policyId).subscribe({
      next: (res) => {
        this.quoteAmounts[policyId] = res.quoteAmount;
        const policy = this.policies.find(p => p.policyId === policyId);
        if (policy) {
          this.quoteBreakdowns[policyId] = this.buildRiskBreakdown(policy);
        }
        this.sending[policyId]      = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.sending[policyId] = false;
        this.errors[policyId]  = 'Failed to calculate quote. Please try again.';
        this.cdr.detectChanges();
      },
    });
  }

  sendQuote(policyId: number): void {
    if (!this.quoteAmounts[policyId]) return;
    this.sending[policyId] = true;
    this.errors[policyId]  = '';

    this.api.sendQuote(policyId, { quoteAmount: this.quoteAmounts[policyId] })
      .subscribe({
        next: () => {
          this.sending[policyId] = false;
          this.successPolicyId   = policyId;
          this.toast.success('Quote sent successfully!');
          this.cdr.detectChanges();
          setTimeout(() => {
            this.policies        = this.policies.filter(p => p.policyId !== policyId);
            this.successPolicyId = null;
            this.cdr.detectChanges();
          }, 1500);
        },
        error: (err) => {
          this.sending[policyId] = false;
          this.errors[policyId]  = err?.error?.message
            || 'Failed to send quote. Please try again.';
          this.cdr.detectChanges();
        },
      });
  }

  // ── Helpers ───────────────────────────────────────────────────────────

  private updatePolicy(updated: PolicyResponse): void {
    const idx = this.policies.findIndex(p => p.policyId === updated.policyId);
    if (idx > -1) this.policies[idx] = updated;
  }

  getKycColor(status: string | undefined): string {
    return status === 'VERIFIED' ? '#10b981'
      : status === 'REJECTED'   ? '#ef4444'
        : '#f59e0b';
  }

  getConditionsList(conditions: string): string[] {
    if (!conditions || conditions === 'SUSPICIOUS_DOCUMENT') return [];
    return conditions.split(',').map(c => c.trim()).filter(c => c);
  }

  getRiskColor(level: string | undefined): string {
    return level === 'LOW'    ? '#10b981'
      : level === 'MEDIUM'   ? '#f59e0b'
        : level === 'HIGH'     ? '#ef4444'
          : level === 'CRITICAL' ? '#7c3aed'
            : '#94a3b8';
  }

  // ── Calculation Helpers ──
  buildRiskBreakdown(policy: PolicyResponse): any[] {
    const factors: any[] = [];
    const members = policy.members || [];
    
    // Base Premium added strictly in UI
    
    // Age Risk
    let maxAge = 30;
    if (members.length) {
      members.forEach((m) => {
        if (m.dateOfBirth) {
          const age = this.calcAge(new Date(m.dateOfBirth));
          if (age > maxAge) maxAge = age;
        }
      });
    }
    const ageFactor = maxAge <= 30 ? 1.0 : maxAge <= 40 ? 1.2 : maxAge <= 50 ? 1.5 : maxAge <= 60 ? 1.8 : 2.2;
    factors.push({
      label: 'Age Risk',
      value: maxAge + ' yrs (oldest member)',
      multiplier: ageFactor,
      icon: 'person',
      color: ageFactor >= 1.8 ? '#dc2626' : ageFactor >= 1.5 ? '#f59e0b' : '#16a34a',
      risk: ageFactor >= 1.8 ? 'High' : ageFactor >= 1.5 ? 'Medium' : 'Low',
    });

    // Health Risk (AI Conditions)
    const hasDisease = policy.extractedConditions && policy.extractedConditions !== 'None' && policy.extractedConditions !== 'SUSPICIOUS_DOCUMENT';
    const diseaseFactor = hasDisease ? 1.3 : 1.0;
    factors.push({
      label: 'Health Risk',
      value: hasDisease ? 'Pre-existing conditions found' : 'No conditions detected',
      multiplier: diseaseFactor,
      icon: 'medical_services',
      color: hasDisease ? '#dc2626' : '#16a34a',
      risk: hasDisease ? 'High' : 'Low',
    });

    // Members Status
    const memberCount = members.length || 1;
    const memberFactor = 1.0 + (memberCount - 1) * 0.7;
    factors.push({
      label: 'Members',
      value: memberCount + ' member(s) covered',
      multiplier: memberFactor,
      icon: 'group',
      color: memberCount >= 4 ? '#f59e0b' : '#16a34a',
      risk: memberCount >= 4 ? 'Medium' : 'Low',
    });

    return factors;
  }

  calcAge(dob: Date): number {
    const today = new Date();
    let age = today.getFullYear() - dob.getFullYear();
    const m = today.getMonth() - dob.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < dob.getDate())) {
      age--;
    }
    return age;
  }
}
