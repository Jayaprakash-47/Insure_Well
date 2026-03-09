import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { InsurancePlan } from '../../../core/models/models';

@Component({ selector: 'app-manage-plans', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './manage-plans.component.html', styleUrl: './manage-plans.component.css' })
export class ManagePlansComponent implements OnInit {
    plans: InsurancePlan[] = [];
    loading = true;
    showModal = false;
    editing = false;
    saving = false;
    editId: number | null = null;
    form = { planName: '', planType: 'INDIVIDUAL', description: '', basePremiumAmount: 0, coverageAmount: 0, planDurationMonths: 12, minAgeLimit: 18, maxAgeLimit: 65, waitingPeriodMonths: 3, maternityCover: false, preExistingDiseaseCover: false };

    constructor(private api: ApiService, private toast: ToastService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void { this.loadPlans(); }

    loadPlans(): void {
        this.api.getAllPlans().subscribe({ next: (d) => { this.plans = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } });
    }

    openCreate(): void { this.resetForm(); this.editing = false; this.editId = null; this.showModal = true; this.cdr.detectChanges(); }

    openEdit(plan: InsurancePlan): void {
        this.form = { planName: plan.planName, planType: plan.planType, description: plan.description, basePremiumAmount: plan.basePremiumAmount, coverageAmount: plan.coverageAmount, planDurationMonths: plan.planDurationMonths, minAgeLimit: plan.minAgeLimit, maxAgeLimit: plan.maxAgeLimit, waitingPeriodMonths: plan.waitingPeriodMonths, maternityCover: plan.maternityCover, preExistingDiseaseCover: plan.preExistingDiseaseCover };
        this.editing = true; this.editId = plan.planId; this.showModal = true; this.cdr.detectChanges();
    }

    save(): void {
        if (!this.form.planName) { this.toast.error('Plan name is required'); return; }
        this.saving = true;
        const action = this.editing && this.editId ? this.api.updatePlan(this.editId, this.form) : this.api.createPlan(this.form);
        action.subscribe({
            next: () => { this.toast.success(this.editing ? 'Plan updated' : 'Plan created'); this.showModal = false; this.saving = false; this.loadPlans(); this.cdr.detectChanges(); },
            error: (err) => { this.saving = false; this.toast.error(err.error?.message || 'Failed'); this.cdr.detectChanges(); }
        });
    }

    formatCurrency(n: number): string { return '₹' + n.toLocaleString('en-IN'); }

    resetForm(): void {
        this.form = { planName: '', planType: 'INDIVIDUAL', description: '', basePremiumAmount: 0, coverageAmount: 0, planDurationMonths: 12, minAgeLimit: 18, maxAgeLimit: 65, waitingPeriodMonths: 3, maternityCover: false, preExistingDiseaseCover: false };
    }
}
