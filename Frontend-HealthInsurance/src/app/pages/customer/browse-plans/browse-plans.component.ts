// FILE: src/app/pages/customer/browse-plans/browse-plans.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { InsurancePlan } from '../../../core/models/models';

@Component({
  selector: 'app-browse-plans',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './browse-plans.component.html',
  styleUrl: './browse-plans.component.css',
})
export class BrowsePlansComponent implements OnInit {
  plans: InsurancePlan[] = [];
  loading = true;
  step = 1;
  selectedPlan: InsurancePlan | null = null;
  submitting = false;
  submitted = false;
  searchTerm = '';
  selectedPlanType = 'ALL';
  planTypes: string[] = [];
  sortBy = 'default';
  minCoverage: number | null = null;
  maxCoverage: number | null = null;
  kycVerified = false;
  bankDetailsFilled = false;
  profileLoading = true;

  // ── e-KYC state ────────────────────────────────────────────────
  kycStep: 'idle' | 'otp-sent' | 'done' = 'idle';
  kycAadhaar = '';
  kycOtp = '';
  kycTransactionId = '';
  kycLoading = false;
  kycError = '';
  kycDone = false;

  today = new Date().toISOString().split('T')[0];

  // ── FIX 3: Plan Comparison ──────────────────────────────────────────────
  selectedForComparison: InsurancePlan[] = [];
  showComparison = false;

  form = {
    nomineeName: '',
    nomineeRelationship: '',
    members: [this.newMember()],
    healthCheckReport: null as File | null,
    aadhaarDocument: null as File | null,   // ── FIX 2: KYC Aadhaar
  };

  errors: any = {};
  touched: any = {};

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.api.getAllPlans().subscribe({
      next: (d) => {
        this.plans = d.filter((p) => p.isActive);
        this.planTypes = [...new Set(this.plans.map((p) => p.planType))];
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });

    this.api.getProfile().subscribe({
      next: (p) => {
        // profile no longer dictates KYC verified state for browsing plans
        this.profileLoading = false;
      },
      error: () => { this.profileLoading = false; }
    });
  }

  get filteredPlans(): InsurancePlan[] {
    let result = this.plans.filter((p) => {
      const matchesSearch =
        !this.searchTerm ||
        p.planName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        p.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesType =
        this.selectedPlanType === 'ALL' || p.planType === this.selectedPlanType;
      const matchesCoverage =
        (!this.minCoverage || p.coverageAmount >= this.minCoverage) &&
        (!this.maxCoverage || p.coverageAmount <= this.maxCoverage);
      return matchesSearch && matchesType && matchesCoverage;
    });

    if (this.sortBy === 'premium_asc')
      result.sort((a, b) => a.basePremiumAmount - b.basePremiumAmount);
    else if (this.sortBy === 'premium_desc')
      result.sort((a, b) => b.basePremiumAmount - a.basePremiumAmount);
    else if (this.sortBy === 'coverage_desc')
      result.sort((a, b) => b.coverageAmount - a.coverageAmount);

    return result;
  }

  setFilter(type: string): void { this.selectedPlanType = type; }

  // ── FIX 1: Individual Plan Bug ──────────────────────────────────────────
  get isIndividualPlan(): boolean {
    return this.selectedPlan?.planType === 'INDIVIDUAL';
  }

  selectPlan(plan: InsurancePlan): void {
    this.selectedPlan = plan;
    this.showComparison = false;

    // FIX 1: Reset to exactly 1 member for INDIVIDUAL plans
    if (plan.planType === 'INDIVIDUAL') {
      this.form.members = [this.newMember()];
    } else {
      this.form.members = [this.newMember()];
    }

    this.step = 2;
    window.scrollTo(0, 0);
  }

  addMember(): void {
    // FIX 1: Prevent adding more than 1 member for INDIVIDUAL plans
    if (this.isIndividualPlan) return;
    this.form.members.push(this.newMember());
  }

  removeMember(i: number): void { this.form.members.splice(i, 1); }

  newMember() {
    return {
      memberName: '',
      relationship: 'SELF',
      dateOfBirth: '',
      gender: '',
      preExistingDiseases: '',
    };
  }

  // ── FIX 2: KYC Aadhaar file handler ────────────────────────────────────
  onAadhaarSelect(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Validate file type
      const allowedTypes = ['image/jpeg', 'image/png', 'application/pdf'];
      if (!allowedTypes.includes(file.type)) {
        this.errors.aadhaarDocument = 'Only PDF, JPG, PNG files are allowed';
        return;
      }
      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.errors.aadhaarDocument = 'File size must be less than 5MB';
        return;
      }
      this.form.aadhaarDocument = file;
      delete this.errors.aadhaarDocument;
    }
  }

  onHealthCheckReportSelect(event: any): void {
    const file = event.target.files[0];
    if (file) {
      this.form.healthCheckReport = file;
      delete this.errors.healthCheckReport;
    }
  }

  // ── FIX 3: Plan Comparison ──────────────────────────────────────────────
  toggleComparison(plan: InsurancePlan, event: Event): void {
    event.stopPropagation(); // prevent selectPlan from firing
    const idx = this.selectedForComparison.findIndex(p => p.planId === plan.planId);
    if (idx > -1) {
      // Already selected — remove it
      this.selectedForComparison.splice(idx, 1);
    } else {
      if (this.selectedForComparison.length >= 3) {
        this.toast.error('You can compare up to 3 plans at a time');
        return;
      }
      this.selectedForComparison.push(plan);
    }
    // Auto-show comparison when 2+ plans selected
    if (this.selectedForComparison.length >= 2) {
      this.showComparison = true;
    } else {
      this.showComparison = false;
    }
  }

  isSelectedForComparison(plan: InsurancePlan): boolean {
    return this.selectedForComparison.some(p => p.planId === plan.planId);
  }

  clearComparison(): void {
    this.selectedForComparison = [];
    this.showComparison = false;
  }

  // ── Validation ──────────────────────────────────────────────────────────
  onBlur(field: string): void {
    this.touched[field] = true;
    this.validateSingleField(field);
  }

  calcAge(dob: string): number {
    const birth = new Date(dob);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  }

  getMinDob(): string {
    if (!this.selectedPlan?.maxAgeLimit) return '';
    const d = new Date();
    d.setFullYear(d.getFullYear() - this.selectedPlan.maxAgeLimit);
    return d.toISOString().split('T')[0];
  }

  getMaxDob(): string {
    if (!this.selectedPlan?.minAgeLimit) return this.today;
    const d = new Date();
    d.setFullYear(d.getFullYear() - this.selectedPlan.minAgeLimit);
    return d.toISOString().split('T')[0];
  }

  validateDob(dob: string, index: number): string | null {
    if (!dob) return 'Date of birth is required';
    const selected = new Date(dob);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (selected > today) return 'Date of birth cannot be a future date';
    if (!this.selectedPlan) return null;
    const age = this.calcAge(dob);
    if (this.selectedPlan.minAgeLimit && age < this.selectedPlan.minAgeLimit)
      return `Member must be at least ${this.selectedPlan.minAgeLimit} years old for this plan`;
    if (this.selectedPlan.maxAgeLimit && age > this.selectedPlan.maxAgeLimit)
      return `Member must be under ${this.selectedPlan.maxAgeLimit} years old for this plan`;
    return null;
  }

  validateSingleField(field: string): void {
    if (field === 'nomineeName') {
      if (!this.form.nomineeName?.trim())
        this.errors.nomineeName = 'Nominee name is required';
      else delete this.errors.nomineeName;
    } else if (field === 'nomineeRelationship') {
      if (!this.form.nomineeRelationship)
        this.errors.nomineeRelationship = 'Nominee relationship is required';
      else delete this.errors.nomineeRelationship;
    } else if (field.startsWith('member')) {
      const match = field.match(/^member(\d+)_(.+)$/);
      if (match) {
        const i = parseInt(match[1]);
        const prop = match[2];
        const m = this.form.members[i];
        if (!m) return;
        if (prop === 'name') {
          if (!m.memberName?.trim()) this.errors[field] = 'Member name is required';
          else delete this.errors[field];
        }
        if (prop === 'relationship') {
          if (!m.relationship) this.errors[field] = 'Relationship is required';
          else delete this.errors[field];
        }
        if (prop === 'dob') {
          const err = this.validateDob(m.dateOfBirth, i);
          if (err) this.errors[field] = err;
          else delete this.errors[field];
        }
        if (prop === 'gender') {
          if (!m.gender) this.errors[field] = 'Gender is required';
          else delete this.errors[field];
        }
      }
    }
  }

  validateForm(): boolean {
    this.errors = {};
    this.touched = { nomineeName: true, nomineeRelationship: true };
    let isValid = true;

    if (!this.form.nomineeName?.trim()) {
      this.errors.nomineeName = 'Nominee name is required';
      isValid = false;
    }
    if (!this.form.nomineeRelationship) {
      this.errors.nomineeRelationship = 'Nominee relationship is required';
      isValid = false;
    }
    if (!this.form.healthCheckReport) {
      this.errors.healthCheckReport = 'Health check report is required';
      isValid = false;
    }

    this.form.members.forEach((m, i) => {
      this.touched[`member${i}_name`] = true;
      this.touched[`member${i}_relationship`] = true;
      this.touched[`member${i}_dob`] = true;
      this.touched[`member${i}_gender`] = true;

      if (!m.memberName?.trim()) {
        this.errors[`member${i}_name`] = 'Member name is required';
        isValid = false;
      }
      if (!m.relationship) {
        this.errors[`member${i}_relationship`] = 'Relationship is required';
        isValid = false;
      }
      const dobErr = this.validateDob(m.dateOfBirth, i);
      if (dobErr) {
        this.errors[`member${i}_dob`] = dobErr;
        isValid = false;
      }
      if (!m.gender) {
        this.errors[`member${i}_gender`] = 'Gender is required';
        isValid = false;
      }
    });

    if (!isValid) {
      this.toast.error('Please fill all required fields correctly');
      setTimeout(() => {
        const firstError = document.querySelector('.error-message');
        if (firstError) firstError.scrollIntoView({ behavior: 'smooth' });
      }, 100);
    }
    return isValid;
  }

  submitRequest(): void {
    if (!this.selectedPlan) return;
    if (!this.validateForm()) return;

    if (!this.kycDone) {
      this.toast.error('Please complete Aadhaar e-KYC verification before submitting.');
      return;
    }

    this.submitting = true;
    const formData = new FormData();
    const req: any = {
      planId: this.selectedPlan.planId,
      nomineeName: this.form.nomineeName,
      nomineeRelationship: this.form.nomineeRelationship?.toUpperCase(),
      kycTransactionId: this.kycTransactionId,
      members: this.form.members.map((m) => ({
        memberName: m.memberName,
        relationship: m.relationship?.toUpperCase(),
        dateOfBirth: m.dateOfBirth,
        gender: m.gender?.toUpperCase(),
        preExistingDiseases: m.preExistingDiseases || null,
      })),
    };

    formData.append('policy',
      new Blob([JSON.stringify(req)], { type: 'application/json' }));
    if (this.form.healthCheckReport) {
      formData.append('healthCheckReport', this.form.healthCheckReport);
    }
    if (this.form.aadhaarDocument) {
      formData.append('aadhaarDocument', this.form.aadhaarDocument);
    }

    this.api.purchasePolicyWithDocument(formData).subscribe({
      next: () => {
        this.submitting = false;
        this.submitted = true;
        this.toast.success('Policy submitted successfully!');
        window.scrollTo(0, 0);
      },
      error: (err: any) => {
        this.submitting = false;
        this.toast.error(err?.error?.message || 'Submission failed. Please try again.');
      },
    });
  }

  // ── e-KYC Methods ───────────────────────────────────────────────────────
  initiateKyc(): void {
    if (this.kycAadhaar !== null && this.kycAadhaar !== undefined) {
      this.kycAadhaar = this.kycAadhaar.toString().trim();
    }
    
    if (!this.kycAadhaar || !/^\d{12}$/.test(this.kycAadhaar)) {
      this.kycError = 'Enter a valid 12-digit Aadhaar number';
      return;
    }
    this.kycLoading = true;
    this.kycError = '';
    this.api.initiateKyc(this.kycAadhaar).subscribe({
      next: (res) => {
        this.kycTransactionId = res.transactionId;
        this.kycStep = 'otp-sent';
        this.kycLoading = false;
        this.toast.success(res.message || 'OTP sent to your registered email. Please check your inbox.');
      },
      error: (err) => {
        this.kycLoading = false;
        this.kycError = err?.error?.message || 'Failed to send OTP';
      }
    });
  }

  verifyKycOtp(): void {
    if (!this.kycOtp || this.kycOtp.length < 6) {
      this.kycError = 'Enter the 6-digit OTP';
      return;
    }
    this.kycLoading = true;
    this.kycError = '';
    this.api.verifyKycOtp(this.kycTransactionId, this.kycOtp).subscribe({
      next: () => {
        this.kycStep = 'done';
        this.kycDone = true;
        this.kycLoading = false;
        this.toast.success('Aadhaar KYC Verified Successfully!');
      },
      error: (err) => {
        this.kycLoading = false;
        this.kycError = err?.error?.message || 'Invalid OTP. Please try again.';
      }
    });
  }

  goToPolicies(): void {
    this.router.navigate(['/customer/policies']);
  }

  formatCoverage(amount?: number): string {
    if (!amount) return '0';
    if (amount >= 100000) return (amount / 100000).toFixed(0) + ' Lakh';
    return amount.toLocaleString('en-IN');
  }
}
