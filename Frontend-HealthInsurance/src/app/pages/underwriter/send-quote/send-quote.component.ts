import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-send-quote',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './send-quote.component.html',
  styleUrl: './send-quote.component.css',
})
export class SendQuoteComponent implements OnInit {
  policy: PolicyResponse | null = null;
  loading = true;
  quoteAmount = 0;
  remarks = '';
  sending = false;
  success = false;
  error = '';
  commissionPct = 10;

  // ── NEW: calculator state ──
  calculating = false;
  calculatorExpanded = true;
  riskBreakdown: RiskFactor[] = [];
  calculatedQuote = 0;
  riskLevel = '';
  riskColor = '';

  get estimatedCommission(): number {
    return Math.round(this.quoteAmount * (this.commissionPct / 100));
  }

  constructor(
    private api: ApiService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    const policyId = this.route.snapshot.paramMap.get('policyId');
    if (policyId) {
      this.api.getPolicyById(+policyId).subscribe({
        next: (p) => {
          this.policy = p;
          this.loading = false;
          // Auto-run calculation when policy loads
          this.autoCalculate();
        },
        error: () => {
          this.loading = false;
        },
      });
    } else {
      this.loading = false;
    }
  }

  // ── NEW: Auto-calculate using backend formula ──
  autoCalculate() {
    if (!this.policy) return;
    this.calculating = true;

    this.api.calculateQuote(this.policy.policyId).subscribe({
      next: (res: any) => {
        this.calculatedQuote = res.calculatedQuote || res;
        this.quoteAmount = this.calculatedQuote;
        this.riskBreakdown = this.buildRiskBreakdown();
        this.setRiskLevel();
        this.calculating = false;
      },
      error: () => {
        // Fallback: calculate locally if API fails
        this.calculatedQuote = this.calculateLocally();
        this.quoteAmount = this.calculatedQuote;
        this.riskBreakdown = this.buildRiskBreakdown();
        this.setRiskLevel();
        this.calculating = false;
      },
    });
  }

  // ── NEW: Local calculation (mirrors backend formula) ──
  private calculateLocally(): number {
    if (!this.policy) return 0;

    const basePremium = this.policy.coverageAmount * 0.02; // 2% of coverage
    const ageFactor = this.getAgeFactor();
    const diseaseFactor = this.getDiseaseFactor();
    const memberFactor = this.getMemberFactor();

    return Math.round(basePremium * ageFactor * diseaseFactor * memberFactor);
  }

  // ── NEW: Risk breakdown builder ──
  private buildRiskBreakdown(): RiskFactor[] {
    if (!this.policy) return [];

    const factors: RiskFactor[] = [];
    const members = this.policy.members || [];

    // Age risk
    const maxAge = this.getMaxAge();
    const ageFactor = this.getAgeFactor();
    factors.push({
      label: 'Age Risk',
      value: maxAge + ' yrs (oldest member)',
      multiplier: ageFactor,
      icon: 'person',
      color: ageFactor >= 1.8 ? '#dc2626' : ageFactor >= 1.5 ? '#f59e0b' : '#16a34a',
      risk: ageFactor >= 1.8 ? 'High' : ageFactor >= 1.5 ? 'Medium' : 'Low',
    });

    // Pre-existing diseases
    const diseaseFactor = this.getDiseaseFactor();
    const hasDisease = this.hasPreExistingDiseases();
    factors.push({
      label: 'Health Risk',
      value: hasDisease ? 'Pre-existing conditions found' : 'No pre-existing conditions',
      multiplier: diseaseFactor,
      icon: 'medical_services',
      color: hasDisease ? '#dc2626' : '#16a34a',
      risk: hasDisease ? 'High' : 'Low',
    });

    // Members count
    const memberCount = members.length || 1;
    const memberFactor = this.getMemberFactor();
    factors.push({
      label: 'Members',
      value: memberCount + ' member' + (memberCount > 1 ? 's' : '') + ' covered',
      multiplier: memberFactor,
      icon: 'group',
      color: memberCount >= 4 ? '#f59e0b' : '#16a34a',
      risk: memberCount >= 4 ? 'Medium' : 'Low',
    });

    // Plan coverage
    factors.push({
      label: 'Coverage Amount',
      value: '₹' + (this.policy.coverageAmount || 0).toLocaleString(),
      multiplier: 1.0,
      icon: 'shield',
      color: '#3b82f6',
      risk: 'Base',
    });

    return factors;
  }

  // ── NEW: Use calculated quote ──
  useCalculatedQuote() {
    this.quoteAmount = this.calculatedQuote;
  }

  // ── NEW: Helper methods ──
  getMaxAge(): number {
    const members = this.policy?.members || [];
    if (!members.length) return 30;
    let maxAge = 0;
    members.forEach((m) => {
      if (m.dateOfBirth) {
        const age = this.calcAge(m.dateOfBirth);
        if (age > maxAge) maxAge = age;
      }
    });
    return maxAge || 30;
  }

  getAgeFactor(): number {
    const age = this.getMaxAge();
    if (age <= 30) return 1.0;
    if (age <= 40) return 1.2;
    if (age <= 50) return 1.5;
    if (age <= 60) return 1.8;
    return 2.2;
  }

  getDiseaseFactor(): number {
    return this.hasPreExistingDiseases() ? 1.3 : 1.0;
  }

  getMemberFactor(): number {
    const count = this.policy?.members?.length || 1;
    return 1.0 + (count - 1) * 0.7;
  }

  hasPreExistingDiseases(): boolean {
    return (this.policy?.members || []).some(
      (m) =>
        m.preExistingDiseases &&
        m.preExistingDiseases.trim() !== '' &&
        m.preExistingDiseases.toLowerCase() !== 'none',
    );
  }

  private calcAge(dob: string): number {
    const birth = new Date(dob);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }

  private setRiskLevel() {
    const score = this.getAgeFactor() * this.getDiseaseFactor();
    if (score >= 2.0) {
      this.riskLevel = 'High Risk';
      this.riskColor = '#dc2626';
    } else if (score >= 1.4) {
      this.riskLevel = 'Medium Risk';
      this.riskColor = '#f59e0b';
    } else {
      this.riskLevel = 'Low Risk';
      this.riskColor = '#16a34a';
    }
  }

  getOverallRiskScore(): number {
    return Math.min(
      100,
      Math.round(
        (this.getAgeFactor() - 1) * 30 +
          (this.getDiseaseFactor() - 1) * 40 +
          (this.getMemberFactor() - 1) * 15,
      ),
    );
  }

  submitQuote() {
    if (!this.policy || !this.quoteAmount) return;
    this.sending = true;
    this.error = '';
    const req = { quoteAmount: this.quoteAmount, remarks: this.remarks };
    this.api.sendQuote(this.policy.policyId, req).subscribe({
      next: () => {
        this.sending = false;
        this.success = true;
        setTimeout(() => this.router.navigate(['/underwriter/pending']), 2000);
      },
      error: (err) => {
        this.sending = false;
        this.error = err?.error?.message || 'Failed to send quote.';
      },
    });
  }
}

export interface RiskFactor {
  label: string;
  value: string;
  multiplier: number;
  icon: string;
  color: string;
  risk: string;
}
