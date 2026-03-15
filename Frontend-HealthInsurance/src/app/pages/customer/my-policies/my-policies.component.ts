import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/services/auth.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-my-policies',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './my-policies.component.html',
  styleUrl: './my-policies.component.css',
})
export class MyPoliciesComponent implements OnInit {
  policies: PolicyResponse[] = [];
  loading = true;

  // ── existing reapply state (unchanged) ──
  showReapplyModal = false;
  reapplyPolicy: PolicyResponse | null = null;
  reapplySubmitting = false;
  reapplyForm = {
    nomineeName: '',
    nomineeRelationship: '',
    members: [this.newMember()],
    healthCheckReport: null as File | null,
  };

  // ── NEW: Renewal modal state ──
  showRenewalModal = false;
  renewalPolicy: PolicyResponse | null = null;
  renewalSubmitting = false;
  renewalForm = {
    nomineeName: '',
    nomineeRelationship: '',
  };
  estimatedRenewalPremium = 0;
  estimatedNcb = 0;

  // ── NEW: PDF download state ──
  downloadingPdf: number | null = null;

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private auth: AuthService, // ← NEW
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadPolicies();
  }

  loadPolicies(): void {
    this.api.getMyPolicies().subscribe({
      next: (d) => {
        this.policies = d;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get quotedPolicies(): PolicyResponse[] {
    return this.policies.filter((p) => p.policyStatus === 'QUOTE_SENT');
  }

  get concernPolicies(): PolicyResponse[] {
    return this.policies.filter((p) => p.policyStatus === 'CONCERN_RAISED');
  }

  // ── NEW: Expired policies needing renewal ──
  get expiredPolicies(): PolicyResponse[] {
    return this.policies.filter((p) => p.policyStatus === 'EXPIRED');
  }

  isEligibleForClaim(policy: PolicyResponse): boolean {
    if (!policy.startDate || !policy.waitingPeriodMonths) return true;
    const eligibilityDate = new Date(policy.startDate);
    eligibilityDate.setMonth(eligibilityDate.getMonth() + policy.waitingPeriodMonths);
    return new Date() >= eligibilityDate;
  }

  getClaimEligibilityDate(policy: PolicyResponse): string {
    if (!policy.startDate || !policy.waitingPeriodMonths) return '';
    const eligibilityDate = new Date(policy.startDate);
    eligibilityDate.setMonth(eligibilityDate.getMonth() + policy.waitingPeriodMonths);
    return eligibilityDate.toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    });
  }

  // ── NEW: Days until expiry ──
  getDaysUntilExpiry(policy: PolicyResponse): number | null {
    if (!policy.endDate) return null;
    const diff = new Date(policy.endDate).getTime() - new Date().getTime();
    return Math.ceil(diff / (1000 * 60 * 60 * 24));
  }

  isExpiringSoon(policy: PolicyResponse): boolean {
    const days = this.getDaysUntilExpiry(policy);
    return days !== null && days <= 30 && days > 0;
  }

  cancel(id: number): void {
    if (!confirm('Cancel this policy?')) return;
    this.api.cancelPolicy(id).subscribe({
      next: () => {
        this.toast.success('Policy cancelled');
        this.loadPolicies();
      },
      error: (err: any) => this.toast.error(err.error?.message || 'Failed'),
    });
  }

  formatCurrency(n: number): string {
    if (!n) return '0';
    return (n || 0).toLocaleString('en-IN');
  }

  getRenewalAgeLoadingAmount(policy: PolicyResponse): number {
    return Math.round((policy.premiumAmount || 0) * 0.03);
  }

  getRenewalNcbAmount(policy: PolicyResponse): number {
    return Math.round(((policy.premiumAmount || 0) * 1.03 * this.estimatedNcb) / 100);
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pending Review',
      ASSIGNED: 'Under Evaluation',
      QUOTE_SENT: 'Quote Ready — Pay Now!',
      CONCERN_RAISED: 'Concern Raised — Action Needed',
      ACTIVE: 'Active',
      CANCELLED: 'Cancelled',
      EXPIRED: 'Expired — Renew Now',
      RENEWED: 'Renewed',
    };
    return labels[status] || status;
  }

  // =================== RENEWAL LOGIC (NEW) ===================

  openRenewalModal(policy: PolicyResponse): void {
    this.renewalPolicy = policy;
    this.renewalForm = {
      nomineeName: policy.nomineeName || '',
      nomineeRelationship: policy.nomineeRelationship || '',
    };

    // Calculate estimated renewal premium (3% increase, NCB discount)
    const base = policy.premiumAmount || 0;
    const afterAging = base * 1.03;

    // NCB: if no claims, add 5% to existing NCB (max 25%)
    const prevNcb = policy.noClaimBonus || 0;
    const hasClaims = (policy.totalClaimedAmount || 0) > 0;
    this.estimatedNcb = hasClaims ? 0 : Math.min(prevNcb + 5, 25);

    const ncbDiscount = afterAging * (this.estimatedNcb / 100);
    this.estimatedRenewalPremium = Math.round(afterAging - ncbDiscount);

    this.showRenewalModal = true;
  }

  closeRenewalModal(): void {
    this.showRenewalModal = false;
    this.renewalPolicy = null;
    this.renewalSubmitting = false;
  }

  submitRenewal(): void {
    if (!this.renewalPolicy) return;

    if (!this.renewalForm.nomineeName?.trim()) {
      this.toast.error('Nominee name is required');
      return;
    }
    if (!this.renewalForm.nomineeRelationship) {
      this.toast.error('Nominee relationship is required');
      return;
    }

    this.renewalSubmitting = true;
    this.api.renewPolicy(this.renewalPolicy.policyId, this.renewalForm).subscribe({
      next: (renewed: PolicyResponse) => {
        this.renewalSubmitting = false;
        this.closeRenewalModal();
        this.toast.success(
          'Policy renewed successfully!' +
            (this.estimatedNcb > 0 ? ' No-Claim Bonus of ' + this.estimatedNcb + '% applied!' : ''),
        );
        this.loadPolicies();
      },
      error: (err: any) => {
        this.renewalSubmitting = false;
        this.toast.error(err?.error?.message || 'Renewal failed. Please try again.');
      },
    });
  }

  // =================== PDF DOWNLOAD (NEW) ===================

  downloadCertificate(policy: PolicyResponse): void {
    this.downloadingPdf = policy.policyId;
    this.auth.downloadPolicyCertificate(policy.policyId);
    setTimeout(() => {
      this.downloadingPdf = null;
    }, 3000);
  }

  // =================== REAPPLY LOGIC (unchanged) ===================

  newMember() {
    return {
      memberName: '',
      relationship: 'SELF',
      dateOfBirth: '',
      gender: '',
      preExistingDiseases: '',
    };
  }

  openReapplyModal(policy: PolicyResponse): void {
    this.reapplyPolicy = policy;
    this.reapplyForm = {
      nomineeName: policy.nomineeName || '',
      nomineeRelationship: policy.nomineeRelationship || '',
      members:
        policy.members && policy.members.length > 0
          ? policy.members.map((m) => ({
              memberName: m.memberName || '',
              relationship: m.relationship || 'SELF',
              dateOfBirth: m.dateOfBirth || '',
              gender: m.gender || '',
              preExistingDiseases: m.preExistingDiseases || '',
            }))
          : [this.newMember()],
      healthCheckReport: null,
    };
    this.showReapplyModal = true;
  }

  closeReapplyModal(): void {
    this.showReapplyModal = false;
    this.reapplyPolicy = null;
    this.reapplySubmitting = false;
  }

  addReapplyMember(): void {
    this.reapplyForm.members.push(this.newMember());
  }

  removeReapplyMember(i: number): void {
    this.reapplyForm.members.splice(i, 1);
  }

  onReapplyFileSelect(event: any): void {
    const file = event.target.files[0];
    if (file) this.reapplyForm.healthCheckReport = file;
  }

  submitReapply(): void {
    if (!this.reapplyPolicy) return;
    if (!this.reapplyForm.nomineeName?.trim()) {
      this.toast.error('Nominee name is required');
      return;
    }
    if (!this.reapplyForm.nomineeRelationship) {
      this.toast.error('Nominee relationship is required');
      return;
    }
    for (let i = 0; i < this.reapplyForm.members.length; i++) {
      const m = this.reapplyForm.members[i];
      if (!m.memberName?.trim() || !m.relationship || !m.dateOfBirth || !m.gender) {
        this.toast.error(`Please fill all required fields for Member ${i + 1}`);
        return;
      }
    }

    this.reapplySubmitting = true;
    const formData = new FormData();
    const req: any = {
      planId: this.reapplyPolicy.planId,
      nomineeName: this.reapplyForm.nomineeName,
      nomineeRelationship: this.reapplyForm.nomineeRelationship?.toUpperCase(),
      members: this.reapplyForm.members.map((m) => ({
        memberName: m.memberName,
        relationship: m.relationship?.toUpperCase(),
        dateOfBirth: m.dateOfBirth,
        gender: m.gender?.toUpperCase(),
        preExistingDiseases: m.preExistingDiseases || null,
      })),
    };
    formData.append('policy', new Blob([JSON.stringify(req)], { type: 'application/json' }));
    if (this.reapplyForm.healthCheckReport) {
      formData.append('healthCheckReport', this.reapplyForm.healthCheckReport);
    }

    this.api.reapplyPolicy(this.reapplyPolicy.policyId, formData).subscribe({
      next: () => {
        this.reapplySubmitting = false;
        this.closeReapplyModal();
        this.toast.success('Policy reapplied successfully! It will be reviewed again.');
        this.loadPolicies();
      },
      error: (err: any) => {
        this.reapplySubmitting = false;
        this.toast.error(err?.error?.message || 'Reapply failed. Please try again.');
      },
    });
  }
}
