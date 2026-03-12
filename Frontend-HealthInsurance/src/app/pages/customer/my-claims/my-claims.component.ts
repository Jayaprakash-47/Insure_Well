import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ClaimResponse, PolicyResponse } from '../../../core/models/models';
@Component({ selector: 'app-my-claims', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './my-claims.component.html', styleUrl: './my-claims.component.css' })
export class MyClaimsComponent implements OnInit {
    claims: ClaimResponse[] = [];
    policies: PolicyResponse[] = [];
    loading = true;
    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }
    ngOnInit(): void {
        this.api.getMyClaims().subscribe({ next: (d) => { this.claims = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } });
        this.api.getMyPolicies().subscribe({ next: (d) => { this.policies = d; this.cdr.detectChanges(); }, error: () => {} });
    }
    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
    getRemainingCoverage(policyId: number): number | null {
        const p = this.policies.find(p => p.policyId === policyId);
        if (!p) return null;
        
        // Backend `remainingCoverage` only deducts SETTLED claims.
        // We dynamically deduct APPROVED or PARTIALLY_APPROVED but not-yet-settled claims for the user.
        let remaining = p.remainingCoverage ?? p.coverageAmount;
        
        const pendingPayouts = this.claims
            .filter(c => c.policyId === policyId && 
                        (c.claimStatus === 'APPROVED' || c.claimStatus === 'PARTIALLY_APPROVED'))
            .reduce((sum, c) => sum + (c.approvedAmount || 0), 0);
            
        return remaining - pendingPayouts;
    }
    
    getCoveragePercent(policyId: number): number {
        const p = this.policies.find(pol => pol.policyId === policyId);
        if (!p || !p.coverageAmount) return 100;
        
        const remaining = Math.max(0, this.getRemainingCoverage(policyId) || 0);
        return Math.round((remaining / p.coverageAmount) * 100);
    }
    getStatusClass(s: string): string {
        const m: Record<string, string> = { SUBMITTED: 'badge-submitted', UNDER_REVIEW: 'badge-info', APPROVED: 'badge-approved', PARTIALLY_APPROVED: 'badge-info', REJECTED: 'badge-rejected', SETTLED: 'badge-success', DOCUMENT_PENDING: 'badge-pending' };
        return m[s] || 'badge-info';
    }
}

