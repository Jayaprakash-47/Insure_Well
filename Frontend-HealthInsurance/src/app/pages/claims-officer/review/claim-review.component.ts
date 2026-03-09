import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { ClaimResponse, ClaimReviewRequest } from '../../../core/models/models';

@Component({
    selector: 'app-claim-review',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
    templateUrl: './claim-review.component.html',
    styleUrl: './claim-review.component.css'
})
export class ClaimReviewComponent implements OnInit {
    claim: ClaimResponse | null = null;
    loading = true;
    submitting = false;

    decision = '';
    approvedAmount: number | null = null;
    reviewerRemarks = '';
    rejectionReason = '';
    escalationReason = '';
    escalationNotes = '';
    additionalDocumentsRequired = '';

    escalationReasons = [
        'HIGH_VALUE_CLAIM', 'FRAUD_SUSPECTED', 'COMPLEX_MEDICAL_CASE',
        'POLICY_INTERPRETATION_NEEDED', 'MULTIPLE_CLAIMS_SAME_PERIOD',
        'PRE_EXISTING_DISEASE_DISPUTE', 'HOSPITAL_NETWORK_ISSUE', 'OTHER'
    ];

    constructor(
        private api: ApiService,
        private toast: ToastService,
        private route: ActivatedRoute,
        private router: Router
    ) { }

    ngOnInit(): void {
        const id = Number(this.route.snapshot.paramMap.get('claimId'));
        this.api.getOfficerClaimDetail(id).subscribe({
            next: (data) => { this.claim = data; this.loading = false; },
            error: () => { this.loading = false; this.toast.error('Failed to load claim'); }
        });
    }

    get canReview(): boolean {
        return this.claim?.claimStatus === 'UNDER_REVIEW' || this.claim?.claimStatus === 'DOCUMENT_PENDING';
    }

    submitReview(): void {
        if (!this.decision) {
            this.toast.error('Please select a decision');
            return;
        }

        const request: ClaimReviewRequest = {
            decision: this.decision,
            reviewerRemarks: this.reviewerRemarks
        };

        if (this.decision === 'APPROVE' || this.decision === 'PARTIALLY_APPROVE') {
            if (!this.approvedAmount || this.approvedAmount <= 0) {
                this.toast.error('Please enter a valid approved amount');
                return;
            }
            request.approvedAmount = this.approvedAmount;
        }

        if (this.decision === 'REJECT') {
            if (!this.rejectionReason.trim()) {
                this.toast.error('Please provide a rejection reason');
                return;
            }
            request.rejectionReason = this.rejectionReason;
        }

        if (this.decision === 'ESCALATE') {
            if (!this.escalationReason) {
                this.toast.error('Please select an escalation reason');
                return;
            }
            request.escalationReason = this.escalationReason;
            request.escalationNotes = this.escalationNotes;
        }

        if (this.decision === 'DOCUMENT_PENDING') {
            request.additionalDocumentsRequired = this.additionalDocumentsRequired;
        }

        this.submitting = true;
        this.api.reviewClaim(this.claim!.claimId, request).subscribe({
            next: (updated) => {
                this.toast.success(`Claim ${updated.claimNumber} — ${this.decision.replace('_', ' ')} successfully`);
                this.router.navigate(['/claims-officer/my-claims']);
            },
            error: (err) => {
                this.toast.error(err.error?.message || 'Failed to submit review');
                this.submitting = false;
            }
        });
    }

    setDecision(d: string): void {
        this.decision = d;
        if (d === 'APPROVE') this.approvedAmount = this.claim?.claimAmount || 0;
        if (d === 'PARTIALLY_APPROVE') this.approvedAmount = null;
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }

    getStatusClass(status: string): string {
        const map: Record<string, string> = {
            'SUBMITTED': 'badge-submitted', 'UNDER_REVIEW': 'badge-info',
            'APPROVED': 'badge-approved', 'REJECTED': 'badge-rejected',
            'ESCALATED': 'badge-escalated', 'DOCUMENT_PENDING': 'badge-pending',
            'PARTIALLY_APPROVED': 'badge-info', 'SETTLED': 'badge-success'
        };
        return map[status] || 'badge-info';
    }
}
