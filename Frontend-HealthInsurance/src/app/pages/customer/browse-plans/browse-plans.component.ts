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
    styleUrl: './browse-plans.component.css'
})
export class BrowsePlansComponent implements OnInit {
    plans: InsurancePlan[] = [];
    loading = true;
    step = 1;
    selectedPlan: InsurancePlan | null = null;
    submitting = false;
    submitted = false;

    form = {
        nomineeName: '',
        nomineeRelationship: '',
        members: [this.newMember()]
    };

    constructor(private api: ApiService, private toast: ToastService, private router: Router) { }

    ngOnInit(): void {
        this.api.getAllPlans().subscribe({
            next: (d) => { this.plans = d.filter(p => p.isActive); this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    selectPlan(plan: InsurancePlan): void {
        this.selectedPlan = plan;
        this.step = 2;
        window.scrollTo(0, 0);
    }

    newMember() {
        return { memberName: '', relationship: 'SELF', dateOfBirth: '', gender: '', preExistingDiseases: '' };
    }

    addMember(): void {
        this.form.members.push(this.newMember());
    }

    removeMember(i: number): void {
        this.form.members.splice(i, 1);
    }

    submitRequest(): void {
        if (!this.selectedPlan) return;
        if (!this.form.nomineeName) { this.toast.error('Please enter nominee name'); return; }
        if (!this.form.nomineeRelationship) { this.toast.error('Please select nominee relationship'); return; }

        const invalidMember = this.form.members.find(m => !m.memberName || !m.dateOfBirth || !m.gender || !m.relationship);
        if (invalidMember) { this.toast.error('Please fill all required fields for each member'); return; }

        this.submitting = true;

        const req: any = {
            planId: this.selectedPlan.planId,
            nomineeName: this.form.nomineeName,
            nomineeRelationship: this.form.nomineeRelationship?.toUpperCase(),
            members: this.form.members.map(m => ({
                memberName: m.memberName,
                relationship: m.relationship?.toUpperCase(),
                dateOfBirth: m.dateOfBirth,
                gender: m.gender?.toUpperCase(),
                preExistingDiseases: m.preExistingDiseases || null
            }))
        };

        this.api.purchasePolicy(req).subscribe({
            next: () => {
                this.submitting = false;
                this.submitted = true;
                window.scrollTo(0, 0);
            },
            error: (err: any) => {
                this.submitting = false;
                this.toast.error(err?.error?.message || 'Submission failed. Please try again.');
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
