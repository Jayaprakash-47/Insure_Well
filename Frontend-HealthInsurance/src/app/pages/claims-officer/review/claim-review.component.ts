import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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
    submitting = false;

    decision = '';
    approvedAmount: number | null = null;
    reviewerRemarks = '';
    rejectionReason = '';
    additionalDocumentsRequired = '';


    constructor(
        private api: ApiService,
        private toast: ToastService,
        private route: ActivatedRoute,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit(): void {
        const id = Number(this.route.snapshot.paramMap.get('claimId'));
        this.api.getOfficerClaimDetail(id).subscribe({
            next: (data) => {
                this.claim = data;
                this.cdr.detectChanges();
            },
            error: (err) => {
                this.toast.error('Failed to load claim: ' + (err.error?.message || err.message || 'Unknown error'));
            }
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

        if (this.decision === 'APPROVED' || this.decision === 'PARTIALLY_APPROVED') {
            if (!this.approvedAmount || this.approvedAmount <= 0) {
                this.toast.error('Please enter a valid approved amount');
                return;
            }
            request.approvedAmount = this.approvedAmount;
        }

        if (this.decision === 'REJECTED') {
            if (!this.rejectionReason.trim()) {
                this.toast.error('Please provide a rejection reason');
                return;
            }
            request.rejectionReason = this.rejectionReason;
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
        if (d === 'APPROVED') this.approvedAmount = this.claim?.claimAmount || 0;
        if (d === 'PARTIALLY_APPROVED') this.approvedAmount = null;
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }

    getStatusClass(status: string): string {
        const map: Record<string, string> = {
            'SUBMITTED': 'badge-submitted', 'UNDER_REVIEW': 'badge-info',
            'APPROVED': 'badge-approved', 'REJECTED': 'badge-rejected',
            'DOCUMENT_PENDING': 'badge-pending',
            'PARTIALLY_APPROVED': 'badge-info', 'SETTLED': 'badge-success'
        };
        return map[status] || 'badge-info';
    }

    viewDocument(doc: any): void {
        if (doc.documentId && this.claim) {
            this.api.downloadDocument(this.claim.claimId, doc.documentId).subscribe({
                next: (blob) => {
                    const url = window.URL.createObjectURL(blob);
                    window.open(url, '_blank');
                },
                error: () => this.toast.error('Failed to view document')
            });
        } else if (doc.filePath) {
            window.open(doc.filePath, '_blank');
        }
    }

    downloadDocument(doc: any): void {
        if (doc.documentId && this.claim) {
            this.api.downloadDocument(this.claim.claimId, doc.documentId).subscribe({
                next: (blob) => {
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = doc.fileName || doc.originalFileName || `Document_${doc.documentId}.pdf`;
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                    window.URL.revokeObjectURL(url);
                },
                error: () => this.toast.error('Failed to download document')
            });
        } else if (doc.filePath) {
            const a = document.createElement('a');
            a.href = doc.filePath;
            a.download = doc.fileName || doc.originalFileName || 'Document';
            a.target = '_blank';
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        }
    }
}
