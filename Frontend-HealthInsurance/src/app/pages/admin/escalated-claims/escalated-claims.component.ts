import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
    selector: 'app-escalated-claims',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './escalated-claims.component.html',
    styleUrl: './escalated-claims.component.css'
})
export class EscalatedClaimsComponent implements OnInit {
    claims: any[] = [];
    loading = true;
    resolving: number | null = null;
    showModal = false;
    selectedClaim: any = null;

    decision = '';
    approvedAmount: number | null = null;
    adminRemarks = '';
    rejectionReason = '';

    constructor(private api: ApiService, private toast: ToastService) { }

    ngOnInit(): void { this.loadClaims(); }

    loadClaims(): void {
        this.loading = true;
        this.api.getEscalatedClaims().subscribe({
            next: (data) => { this.claims = data; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    openResolve(claim: any): void {
        this.selectedClaim = claim;
        this.decision = '';
        this.approvedAmount = claim.claimAmount;
        this.adminRemarks = '';
        this.rejectionReason = '';
        this.showModal = true;
    }

    resolve(): void {
        if (!this.decision || !this.adminRemarks.trim()) {
            this.toast.error('Please select a decision and provide remarks');
            return;
        }

        this.resolving = this.selectedClaim.claimId;
        this.api.resolveEscalatedClaim(this.selectedClaim.claimId, {
            decision: this.decision,
            approvedAmount: this.decision === 'APPROVE' ? this.approvedAmount || 0 : undefined,
            adminRemarks: this.adminRemarks,
            rejectionReason: this.decision === 'REJECT' ? this.rejectionReason : undefined
        }).subscribe({
            next: () => {
                this.toast.success('Escalated claim resolved');
                this.showModal = false;
                this.loadClaims();
            },
            error: (err) => {
                this.toast.error(err.error?.message || 'Resolution failed');
                this.resolving = null;
            }
        });
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }
}
