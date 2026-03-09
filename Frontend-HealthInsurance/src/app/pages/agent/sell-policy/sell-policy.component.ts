import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { InsurancePlan, AgentSellPolicyRequest, PolicyMemberRequest } from '../../../core/models/models';

@Component({
    selector: 'app-sell-policy',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './sell-policy.component.html',
    styleUrl: './sell-policy.component.css'
})
export class SellPolicyComponent implements OnInit {
    step = 1;
    customers: any[] = [];
    plans: InsurancePlan[] = [];
    loading = true;
    submitting = false;

    selectedCustomerId: number | null = null;
    selectedPlanId: number | null = null;
    nomineeName = '';
    nomineeRelationship = '';
    members: PolicyMemberRequest[] = [];

    constructor(private api: ApiService, private toast: ToastService, private router: Router) { }

    ngOnInit(): void {
        this.api.getAgentCustomers().subscribe({
            next: (data) => { this.customers = data; },
            error: () => { this.toast.error('Failed to load customers'); }
        });
        this.api.getAgentPlans().subscribe({
            next: (data) => { this.plans = data; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    get selectedCustomer(): any {
        return this.customers.find(c => c.userId === this.selectedCustomerId);
    }

    get selectedPlan(): InsurancePlan | undefined {
        return this.plans.find(p => p.planId === this.selectedPlanId);
    }

    nextStep(): void {
        if (this.step === 1 && !this.selectedCustomerId) {
            this.toast.error('Please select a customer');
            return;
        }
        if (this.step === 2 && !this.selectedPlanId) {
            this.toast.error('Please select a plan');
            return;
        }
        if (this.step === 3 && !this.nomineeName.trim()) {
            this.toast.error('Please enter nominee details');
            return;
        }
        this.step++;
    }

    prevStep(): void {
        if (this.step > 1) this.step--;
    }

    addMember(): void {
        this.members.push({ memberName: '', relationship: '', dateOfBirth: '', gender: '', preExistingDiseases: '' });
    }

    removeMember(i: number): void {
        this.members.splice(i, 1);
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }

    submitSale(): void {
        const req: AgentSellPolicyRequest = {
            customerId: this.selectedCustomerId!,
            planId: this.selectedPlanId!,
            nomineeName: this.nomineeName,
            nomineeRelationship: this.nomineeRelationship,
            members: this.members.filter(m => m.memberName.trim())
        };

        this.submitting = true;
        this.api.agentSellPolicy(req).subscribe({
            next: (policy) => {
                this.toast.success(`Policy ${policy.policyNumber} sold successfully!`);
                this.router.navigate(['/agent/policies']);
            },
            error: (err) => {
                this.toast.error(err.error?.message || 'Failed to sell policy');
                this.submitting = false;
            }
        });
    }
}
