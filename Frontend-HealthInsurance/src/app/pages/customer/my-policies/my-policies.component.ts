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

  // ── Reapply modal ──
  showReapplyModal  = false;
  reapplyPolicy: PolicyResponse | null = null;
  reapplySubmitting = false;
  reapplyForm = {
    nomineeName: '',
    nomineeRelationship: '',
    members: [this.newMember()],
    healthCheckReport: null as File | null,
    // FIX: Added Aadhaar field for KYC rejection re-upload
    aadhaarDocument: null as File | null,
  };

  // ── Renewal modal ──
  showRenewalModal  = false;
  renewalPolicy: PolicyResponse | null = null;
  renewalSubmitting = false;
  renewalForm = { nomineeName: '', nomineeRelationship: '' };
  estimatedRenewalPremium = 0;
  estimatedNcb = 0;

  // ── PDF download ──
  downloadingPdf: number | null = null;

  // ── Premium breakdown toggle ──
  expandedBreakdown: number | null = null;

  toggleBreakdown(policyId: number): void {
    this.expandedBreakdown = this.expandedBreakdown === policyId ? null : policyId;
  }

  // ──── Premium breakdown helpers (mirrors underwriter formula) ────

  getOldestMemberAge(p: PolicyResponse): number {
    if (!p.members?.length) return 30;
    let maxAge = 0;
    for (const m of p.members) {
      if (m.dateOfBirth) {
        const birth = new Date(m.dateOfBirth);
        const today = new Date();
        let age = today.getFullYear() - birth.getFullYear();
        if (today.getMonth() < birth.getMonth() ||
            (today.getMonth() === birth.getMonth() && today.getDate() < birth.getDate())) age--;
        if (age > maxAge) maxAge = age;
      }
    }
    return maxAge || 30;
  }

  getAgeMultiplier(p: PolicyResponse): number {
    const age = this.getOldestMemberAge(p);
    if (age <= 30) return 1.0;
    if (age <= 40) return 1.2;
    if (age <= 50) return 1.5;
    if (age <= 60) return 1.8;
    return 2.2;
  }

  getAgeRisk(p: PolicyResponse): string {
    const m = this.getAgeMultiplier(p);
    if (m >= 1.8) return 'High';
    if (m >= 1.5) return 'Medium';
    return 'Low';
  }

  getHealthMultiplier(p: PolicyResponse): number {
    const hasDisease = (p.members || []).some(m =>
      m.preExistingDiseases &&
      m.preExistingDiseases.trim() !== '' &&
      m.preExistingDiseases.toLowerCase() !== 'none');
    // Also check extractedConditions from AI analysis
    const hasExtracted = p.extractedConditions &&
      p.extractedConditions.trim() !== '' &&
      p.extractedConditions.toLowerCase() !== 'none' &&
      p.extractedConditions.toLowerCase() !== 'n/a';
    return (hasDisease || hasExtracted) ? 1.3 : 1.0;
  }

  getHealthRiskLabel(p: PolicyResponse): string {
    if (this.getHealthMultiplier(p) > 1.0) return 'Pre-existing conditions found';
    return 'No pre-existing conditions';
  }

  getMemberMultiplier(p: PolicyResponse): number {
    const count = p.members?.length || 1;
    return 1.0 + (count - 1) * 0.7;
  }

  getMemberRisk(p: PolicyResponse): string {
    const m = this.getMemberMultiplier(p);
    if (m > 2.0) return 'High';
    if (m > 1.5) return 'Medium';
    return 'Low';
  }

  getRiskScore(p: PolicyResponse): number {
    return Math.min(100, Math.round(
      (this.getAgeMultiplier(p) - 1) * 30 +
      (this.getHealthMultiplier(p) - 1) * 40 +
      (this.getMemberMultiplier(p) - 1) * 15
    ));
  }

  getRiskLabel(p: PolicyResponse): string {
    const score = this.getAgeMultiplier(p) * this.getHealthMultiplier(p);
    if (score >= 2.0) return 'High Risk';
    if (score >= 1.4) return 'Medium Risk';
    return 'Low Risk';
  }

  getRiskColor(p: PolicyResponse): string {
    const score = this.getAgeMultiplier(p) * this.getHealthMultiplier(p);
    if (score >= 2.0) return '#dc2626';
    if (score >= 1.4) return '#f59e0b';
    return '#16a34a';
  }

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void { this.loadPolicies(); }

  loadPolicies(): void {
    this.api.getMyPolicies().subscribe({
      next: (d) => { this.policies = d; this.loading = false; this.cdr.detectChanges(); },
      error: ()  => { this.loading = false; this.cdr.detectChanges(); },
    });
  }

  get quotedPolicies():  PolicyResponse[] { return this.policies.filter(p => p.policyStatus === 'QUOTE_SENT'); }
  get concernPolicies(): PolicyResponse[] { return this.policies.filter(p => p.policyStatus === 'CONCERN_RAISED'); }
  get expiredPolicies(): PolicyResponse[] { return this.policies.filter(p => p.policyStatus === 'EXPIRED'); }

  // FIX: Helper to detect if concern is specifically a KYC rejection
  isKycRejection(policy: PolicyResponse): boolean {
    return policy.kycStatus === 'REJECTED'
      || (policy.underwriterRemarks?.startsWith('KYC Rejected') ?? false);
  }

  isEligibleForClaim(policy: PolicyResponse): boolean {
    if (!policy.startDate || !policy.waitingPeriodMonths) return true;
    const eligibilityDate = new Date(policy.startDate);
    eligibilityDate.setMonth(eligibilityDate.getMonth() + policy.waitingPeriodMonths);
    return new Date() >= eligibilityDate;
  }

  getClaimEligibilityDate(policy: PolicyResponse): string {
    if (!policy.startDate || !policy.waitingPeriodMonths) return '';
    const d = new Date(policy.startDate);
    d.setMonth(d.getMonth() + policy.waitingPeriodMonths);
    return d.toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' });
  }

  getDaysUntilExpiry(policy: PolicyResponse): number | null {
    if (!policy.endDate) return null;
    return Math.ceil((new Date(policy.endDate).getTime() - new Date().getTime()) / 86400000);
  }

  isExpiringSoon(policy: PolicyResponse): boolean {
    const d = this.getDaysUntilExpiry(policy);
    return d !== null && d <= 30 && d > 0;
  }

  cancel(id: number): void {
    if (!confirm('Cancel this policy?')) return;
    this.api.cancelPolicy(id).subscribe({
      next: () => { this.toast.success('Policy cancelled'); this.loadPolicies(); },
      error: (err: any) => this.toast.error(err.error?.message || 'Failed'),
    });
  }

  formatCurrency(n: number): string { return !n ? '0' : (n || 0).toLocaleString('en-IN'); }

  getRenewalAgeLoadingAmount(p: PolicyResponse): number { return Math.round((p.premiumAmount || 0) * 0.03); }
  getRenewalNcbAmount(p: PolicyResponse): number {
    return Math.round(((p.premiumAmount || 0) * 1.03 * this.estimatedNcb) / 100);
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'Pending Review',
      ASSIGNED: 'Under Evaluation',
      QUOTE_SENT: 'Quote Ready — Pay Now!',
      CONCERN_RAISED: 'Action Needed',
      ACTIVE: 'Active',
      CANCELLED: 'Cancelled',
      EXPIRED: 'Expired — Renew Now',
      RENEWED: 'Renewed',
    };
    return labels[status] || status;
  }

  // =================== RENEWAL ===================

  openRenewalModal(policy: PolicyResponse): void {
    this.renewalPolicy = policy;
    this.renewalForm = {
      nomineeName: policy.nomineeName || '',
      nomineeRelationship: policy.nomineeRelationship || '',
    };
    const base = policy.premiumAmount || 0;
    const afterAging = base * 1.03;
    const prevNcb = policy.noClaimBonus || 0;
    const hasClaims = (policy.totalClaimedAmount || 0) > 0;
    this.estimatedNcb = hasClaims ? 0 : Math.min(prevNcb + 5, 25);
    this.estimatedRenewalPremium = Math.round(afterAging - afterAging * (this.estimatedNcb / 100));
    this.showRenewalModal = true;
  }

  closeRenewalModal(): void { this.showRenewalModal = false; this.renewalPolicy = null; this.renewalSubmitting = false; }

  submitRenewal(): void {
    if (!this.renewalPolicy) return;
    if (!this.renewalForm.nomineeName?.trim())   { this.toast.error('Nominee name is required'); return; }
    if (!this.renewalForm.nomineeRelationship)    { this.toast.error('Nominee relationship is required'); return; }

    this.renewalSubmitting = true;
    this.api.renewPolicy(this.renewalPolicy.policyId, this.renewalForm).subscribe({
      next: () => {
        this.renewalSubmitting = false;
        this.closeRenewalModal();
        this.toast.success('Policy renewed successfully!' +
          (this.estimatedNcb > 0 ? ' No-Claim Bonus of ' + this.estimatedNcb + '% applied!' : ''));
        this.loadPolicies();
      },
      error: (err: any) => { this.renewalSubmitting = false; this.toast.error(err?.error?.message || 'Renewal failed.'); },
    });
  }

  // =================== PDF CERTIFICATE ===================

  downloadCertificate(policy: PolicyResponse): void {
    this.downloadingPdf = policy.policyId;
    this.auth.downloadPolicyCertificate(policy.policyId);
    setTimeout(() => { this.downloadingPdf = null; }, 3000);
  }

  // =================== REAPPLY ===================

  newMember() {
    return { memberName: '', relationship: 'SELF', dateOfBirth: '', gender: '', preExistingDiseases: '' };
  }

  openReapplyModal(policy: PolicyResponse): void {
    this.reapplyPolicy = policy;
    this.reapplyForm = {
      nomineeName: policy.nomineeName || '',
      nomineeRelationship: policy.nomineeRelationship || '',
      members: policy.members && policy.members.length > 0
        ? policy.members.map(m => ({
          memberName: m.memberName || '',
          relationship: m.relationship || 'SELF',
          dateOfBirth: m.dateOfBirth || '',
          gender: m.gender || '',
          preExistingDiseases: m.preExistingDiseases || '',
        }))
        : [this.newMember()],
      healthCheckReport: null,
      aadhaarDocument: null,  // FIX: reset Aadhaar on open
    };
    this.showReapplyModal = true;
  }

  closeReapplyModal(): void { this.showReapplyModal = false; this.reapplyPolicy = null; this.reapplySubmitting = false; }

  addReapplyMember(): void { this.reapplyForm.members.push(this.newMember()); }
  removeReapplyMember(i: number): void { this.reapplyForm.members.splice(i, 1); }

  onReapplyFileSelect(event: any): void {
    const file = event.target.files[0];
    if (file) this.reapplyForm.healthCheckReport = file;
  }

  // FIX: Aadhaar file selection handler
  onAadhaarFileSelect(event: any): void {
    const file = event.target.files[0];
    if (file) this.reapplyForm.aadhaarDocument = file;
  }

  submitReapply(): void {
    if (!this.reapplyPolicy) return;
    if (!this.reapplyForm.nomineeName?.trim())   { this.toast.error('Nominee name is required'); return; }
    if (!this.reapplyForm.nomineeRelationship)    { this.toast.error('Nominee relationship is required'); return; }

    // FIX: If KYC was rejected, require a new Aadhaar document
    if (this.isKycRejection(this.reapplyPolicy) && !this.reapplyForm.aadhaarDocument) {
      this.toast.error('Please upload a new Aadhaar document to re-verify your KYC');
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
      members: this.reapplyForm.members.map(m => ({
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

    // FIX: Append Aadhaar document if provided
    if (this.reapplyForm.aadhaarDocument) {
      formData.append('aadhaarDocument', this.reapplyForm.aadhaarDocument);
    }

    this.api.reapplyPolicy(this.reapplyPolicy.policyId, formData).subscribe({
      next: () => {
        this.reapplySubmitting = false;
        this.closeReapplyModal();
        this.toast.success('Reapplication submitted successfully! It will be reviewed again.');
        this.loadPolicies();
      },
      error: (err: any) => {
        this.reapplySubmitting = false;
        this.toast.error(err?.error?.message || 'Reapply failed. Please try again.');
      },
    });
  }
}
