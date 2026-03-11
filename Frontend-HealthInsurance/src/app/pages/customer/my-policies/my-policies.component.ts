import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
    selector: 'app-my-policies',
    standalone: true,
    imports: [RouterLink],
    templateUrl: './my-policies.component.html',
    styleUrl: './my-policies.component.css'
})
export class MyPoliciesComponent implements OnInit {
    policies: PolicyResponse[] = [];
    loading = true;

    constructor(private api: ApiService, private toast: ToastService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void { this.loadPolicies(); }

    loadPolicies(): void {
        this.api.getMyPolicies().subscribe({
            next: (d) => { this.policies = d; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    get quotedPolicies(): PolicyResponse[] {
        return this.policies.filter(p => p.policyStatus === 'QUOTE_SENT');
    }

    isEligibleForClaim(policy: PolicyResponse): boolean {
        if (!policy.startDate || !policy.waitingPeriodMonths) return true;
        const startDate = new Date(policy.startDate);
        const eligibilityDate = new Date(startDate);
        eligibilityDate.setMonth(eligibilityDate.getMonth() + policy.waitingPeriodMonths);
        return new Date() >= eligibilityDate;
    }

    getClaimEligibilityDate(policy: PolicyResponse): string {
        if (!policy.startDate || !policy.waitingPeriodMonths) return '';
        const startDate = new Date(policy.startDate);
        const eligibilityDate = new Date(startDate);
        eligibilityDate.setMonth(eligibilityDate.getMonth() + policy.waitingPeriodMonths);
        return eligibilityDate.toLocaleDateString('en-IN', { year: 'numeric', month: 'long', day: 'numeric' });
    }

    cancel(id: number): void {
        if (!confirm('Cancel this policy?')) return;
        this.api.cancelPolicy(id).subscribe({
            next: () => { this.toast.success('Policy cancelled'); this.loadPolicies(); },
            error: (err: any) => this.toast.error(err.error?.message || 'Failed')
        });
    }

    formatCurrency(n: number): string {
        if (!n) return '0';
        return (n || 0).toLocaleString('en-IN');
    }

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            'PENDING': 'Pending Review',
            'ASSIGNED': 'Under Evaluation',
            'QUOTE_SENT': 'Quote Ready — Pay Now!',
            'ACTIVE': 'Active',
            'CANCELLED': 'Cancelled',
            'EXPIRED': 'Expired',
            'RENEWED': 'Renewed'
        };
        return labels[status] || status;
    }
}
