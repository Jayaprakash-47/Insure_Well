import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { ClaimResponse } from '../../../core/models/models';

@Component({
    selector: 'app-claim-queue',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './claim-queue.component.html',
    styleUrl: './claim-queue.component.css'
})
export class ClaimQueueComponent implements OnInit {
    claims: ClaimResponse[] = [];
    loading = true;
    pickingUp: number | null = null;

    constructor(private api: ApiService, private toast: ToastService, private router: Router) { }

    ngOnInit(): void {
        this.loadQueue();
    }

    loadQueue(): void {
        this.loading = true;
        this.api.getClaimQueue().subscribe({
            next: (data) => { this.claims = data; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    pickupClaim(claimId: number): void {
        this.pickingUp = claimId;
        this.api.pickupClaim(claimId).subscribe({
            next: (claim) => {
                this.toast.success(`Claim ${claim.claimNumber} assigned to you`);
                this.router.navigate(['/claims-officer/review', claimId]);
            },
            error: (err) => {
                this.toast.error(err.error?.message || 'Failed to pick up claim');
                this.pickingUp = null;
            }
        });
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }

    getStatusClass(status: string): string {
        const map: Record<string, string> = {
            'SUBMITTED': 'badge-submitted', 'PENDING': 'badge-pending',
            'UNDER_REVIEW': 'badge-info', 'APPROVED': 'badge-approved',
            'REJECTED': 'badge-rejected', 'ESCALATED': 'badge-escalated',
            'DOCUMENT_PENDING': 'badge-pending', 'SETTLED': 'badge-success'
        };
        return map[status] || 'badge-info';
    }
}
