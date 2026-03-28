// FILE: src/app/pages/underwriter/apply-for-customer/apply-for-customer.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { InsurancePlan } from '../../../core/models/models';

interface CustomerSummary {
  userId: number;
  name: string;
  email: string;
  phone: string;
  city: string;
  dateOfBirth: string;
  totalPolicies: number;
}

interface AgentRequest {
  id: number;
  customerName: string;
  customerEmail: string;
  customerPhone: string;
  planTypeInterest: string;
  preferredTime: string;
  message: string;
  status: string;
}

@Component({
  selector: 'app-apply-for-customer',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './apply-for-customer.html',
  styleUrl: './apply-for-customer.css',
})
export class ApplyForCustomerComponent implements OnInit {

  // ── Wizard state ──────────────────────────────────────────────────────
  step = 1;
  requestId: number | null = null;
  agentRequest: AgentRequest | null = null;

  // Step 1 — Customer
  customers: CustomerSummary[] = [];
  customerSearch = '';
  selectedCustomer: CustomerSummary | null = null;
  loadingCustomers = true;

  // Step 2 — Plan
  plans: InsurancePlan[] = [];
  selectedPlan: InsurancePlan | null = null;
  planSearch = '';
  loadingPlans = false;

  // Step 3 — Policy details
  today = new Date().toISOString().split('T')[0];
  form = {
    nomineeName: '',
    nomineeRelationship: '',
    members: [this.newMember()]
  };
  errors: any = {};

  // Step 4 — Submit
  submitting = false;
  submitted = false;
  resultPolicy: any = null;

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private route: ActivatedRoute,
    protected router: Router
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['requestId']) {
        this.requestId = +params['requestId'];
        this.loadRequestDetails(this.requestId);
      }
    });
    this.loadCustomers();
    this.loadPlans();
  }

  loadRequestDetails(id: number): void {
    this.api.getAgentRequests().subscribe({
      next: (requests: AgentRequest[]) => {
        this.agentRequest = requests.find(r => r.id === id) || null;
      },
      error: () => {}
    });
  }

  loadCustomers(): void {
    this.loadingCustomers = true;
    // ── FIX: use underwriter endpoint, not admin endpoint ────────────────
    this.api.getUnderwriterCustomers().subscribe({
      next: (data) => {
        this.customers = data;
        this.loadingCustomers = false;
      },
      error: () => {
        this.loadingCustomers = false;
        this.toast.error('Failed to load customers');
      }
    });
  }

  loadPlans(): void {
    this.loadingPlans = true;
    this.api.getAllPlans().subscribe({
      next: (data) => {
        this.plans = data.filter((p: InsurancePlan) => p.isActive);
        this.loadingPlans = false;
      },
      error: () => { this.loadingPlans = false; }
    });
  }

  // ── Step 1: Customer search ───────────────────────────────────────────
  get filteredCustomerList(): CustomerSummary[] {
    if (!this.customerSearch.trim()) return this.customers;
    const q = this.customerSearch.toLowerCase();
    return this.customers.filter(c =>
      c.name.toLowerCase().includes(q) ||
      c.email.toLowerCase().includes(q) ||
      (c.phone && c.phone.includes(q))
    );
  }

  selectCustomer(c: CustomerSummary): void {
    this.selectedCustomer = c;
  }

  // ── Step 2: Plan selection ────────────────────────────────────────────
  get filteredPlanList(): InsurancePlan[] {
    if (!this.planSearch.trim()) return this.plans;
    const q = this.planSearch.toLowerCase();
    return this.plans.filter(p =>
      p.planName.toLowerCase().includes(q) ||
      p.planType.toLowerCase().includes(q)
    );
  }

  selectPlan(plan: InsurancePlan): void {
    this.selectedPlan = plan;
    if (plan.planType === 'INDIVIDUAL') {
      this.form.members = [this.newMember()];
    }
  }

  get isIndividualPlan(): boolean {
    return this.selectedPlan?.planType === 'INDIVIDUAL';
  }

  // ── Step 3: Form ──────────────────────────────────────────────────────
  newMember() {
    return {
      memberName: '',
      relationship: 'SELF',
      dateOfBirth: '',
      gender: '',
      preExistingDiseases: ''
    };
  }

  addMember(): void {
    if (this.isIndividualPlan) return;
    this.form.members.push(this.newMember());
  }

  removeMember(i: number): void { this.form.members.splice(i, 1); }

  calcAge(dob: string): number {
    if (!dob) return 0;
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

  // ── Step navigation ───────────────────────────────────────────────────
  nextStep(): void {
    if (this.step === 1 && !this.selectedCustomer) {
      this.toast.error('Please select a customer'); return;
    }
    if (this.step === 2 && !this.selectedPlan) {
      this.toast.error('Please select a plan'); return;
    }
    if (this.step === 3 && !this.validateForm()) return;
    this.step++;
  }

  prevStep(): void { if (this.step > 1) this.step--; }

  validateForm(): boolean {
    this.errors = {};
    let valid = true;
    if (!this.form.nomineeName?.trim()) {
      this.errors.nomineeName = 'Required'; valid = false;
    }
    if (!this.form.nomineeRelationship) {
      this.errors.nomineeRelationship = 'Required'; valid = false;
    }
    this.form.members.forEach((m, i) => {
      if (!m.memberName?.trim()) {
        this.errors[`m${i}_name`] = 'Required'; valid = false;
      }
      if (!m.dateOfBirth) {
        this.errors[`m${i}_dob`] = 'Required'; valid = false;
      }
      if (!m.gender) {
        this.errors[`m${i}_gender`] = 'Required'; valid = false;
      }
    });
    if (!valid) this.toast.error('Please fill all required fields');
    return valid;
  }

  // ── Submit ────────────────────────────────────────────────────────────
  submitPolicy(): void {
    if (!this.selectedCustomer || !this.selectedPlan) return;
    this.submitting = true;

    const req = {
      planId: this.selectedPlan.planId,
      nomineeName: this.form.nomineeName,
      nomineeRelationship: this.form.nomineeRelationship.toUpperCase(),
      members: this.form.members.map(m => ({
        memberName: m.memberName,
        relationship: m.relationship.toUpperCase(),
        dateOfBirth: m.dateOfBirth,
        gender: m.gender.toUpperCase(),
        preExistingDiseases: m.preExistingDiseases || null
      }))
    };

    const applyFn = this.requestId
      ? this.api.applyForCustomerViaRequest(this.requestId, req)
      : this.api.applyForCustomerDirect(this.selectedCustomer.userId, req);

    applyFn.subscribe({
      next: (policy: any) => {
        this.submitting = false;
        this.submitted = true;
        this.resultPolicy = policy;
        this.toast.success(
          `Policy ${policy.policyNumber} submitted for ${this.selectedCustomer?.name}`
        );
      },
      error: (err: any) => {
        this.submitting = false;
        this.toast.error(err?.error?.message || 'Submission failed');
      }
    });
  }

  formatCoverage(amount?: number): string {
    if (!amount) return '0';
    if (amount >= 100000) return (amount / 100000).toFixed(0) + ' Lakh';
    return amount.toLocaleString('en-IN');
  }

  goToDashboard(): void {
    this.router.navigate(['/underwriter/dashboard']);
  }
}
