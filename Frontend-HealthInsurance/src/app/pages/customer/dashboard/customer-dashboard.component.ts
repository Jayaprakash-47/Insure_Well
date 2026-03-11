import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { PolicyResponse, ClaimResponse } from '../../../core/models/models';

@Component({ selector: 'app-customer-dashboard', standalone: true, imports: [RouterLink], templateUrl: './customer-dashboard.component.html', styleUrl: './customer-dashboard.component.css' })
export class CustomerDashboardComponent implements OnInit {
    policies: PolicyResponse[] = [];
    claims: ClaimResponse[] = [];
    loading = true;

    constructor(private api: ApiService, public auth: AuthService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getMyPolicies().subscribe({ next: (d) => { this.policies = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } });
        this.api.getMyClaims().subscribe({ next: (d) => { this.claims = d; this.cdr.detectChanges(); } });
    }

    get activePolicies(): number { return this.policies.filter(p => p.policyStatus === 'ACTIVE').length; }
    get pendingPolicies(): number { return this.policies.filter(p => ['PENDING', 'ASSIGNED', 'QUOTE_SENT'].includes(p.policyStatus)).length; }
    get pendingClaims(): number { return this.claims.filter(c => c.claimStatus === 'SUBMITTED').length; }
    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
}
