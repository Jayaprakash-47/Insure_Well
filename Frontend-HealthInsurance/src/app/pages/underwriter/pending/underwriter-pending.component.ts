import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
    selector: 'app-underwriter-pending',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './underwriter-pending.component.html',
    styleUrl: './underwriter-pending.component.css'
})
export class UnderwriterPendingComponent implements OnInit {
    policies: PolicyResponse[] = [];
    loading = true;
    quoteAmounts: { [policyId: number]: number } = {};
    sending: { [policyId: number]: boolean } = {};
    errors: { [policyId: number]: string } = {};
    successPolicyId: number | null = null;
    showConcernForm: { [policyId: number]: boolean } = {};
    concernRemarks: { [policyId: number]: string } = {};
    sendingConcern: { [policyId: number]: boolean } = {};

    constructor(private api: ApiService, private router: Router, private cdr: ChangeDetectorRef, private toast: ToastService) { }

    ngOnInit() {
        this.api.getUnderwriterPendingAssignments().subscribe({
            next: (data) => { this.policies = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    downloadHealthReport(policyId: number): void {
        this.api.getPolicyDocument(policyId).subscribe({
            next: (blob) => {
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `HealthReport_Policy_${policyId}.pdf`;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                window.URL.revokeObjectURL(url);
            },
            error: () => this.toast.error('No health report available to download')
        });
    }

    toggleConcernForm(policyId: number): void {
        this.showConcernForm[policyId] = !this.showConcernForm[policyId];
        if (!this.concernRemarks[policyId]) this.concernRemarks[policyId] = '';
    }

    submitConcern(policyId: number): void {
        const remarks = this.concernRemarks[policyId]?.trim();
        if (!remarks) {
            this.toast.error('Please provide concern details');
            return;
        }
        this.sendingConcern[policyId] = true;
        this.api.raiseConcern(policyId, remarks).subscribe({
            next: () => {
                this.toast.success('Concern raised successfully');
                this.showConcernForm[policyId] = false;
                this.sendingConcern[policyId] = false;
                this.policies = this.policies.filter(p => p.policyId !== policyId);
                this.cdr.detectChanges();
            },
            error: (err) => {
                this.sendingConcern[policyId] = false;
                this.toast.error(err?.error?.message || 'Failed to raise concern');
                this.cdr.detectChanges();
            }
        });
    }

    calculateQuote(policyId: number) {
        this.sending[policyId] = true;
        this.errors[policyId] = '';
        this.api.calculateUnderwriterQuote(policyId).subscribe({
            next: (res) => {
                this.quoteAmounts[policyId] = res.quoteAmount;
                this.sending[policyId] = false;
                this.cdr.detectChanges();
            },
            error: (err) => {
                this.sending[policyId] = false;
                this.errors[policyId] = 'Failed to calculate quote. Please try again.';
                this.cdr.detectChanges();
            }
        });
    }

    sendQuote(policyId: number) {
        if (!this.quoteAmounts[policyId]) return;
        this.sending[policyId] = true;
        this.errors[policyId] = '';
        const req = { quoteAmount: this.quoteAmounts[policyId] };
        this.api.sendQuote(policyId, req).subscribe({
            next: () => {
                this.sending[policyId] = false;
                this.successPolicyId = policyId;
                this.cdr.detectChanges();
                setTimeout(() => {
                    this.policies = this.policies.filter(p => p.policyId !== policyId);
                    this.successPolicyId = null;
                    this.cdr.detectChanges();
                }, 1500);
            },
            error: (err) => {
                this.sending[policyId] = false;
                this.errors[policyId] = err?.error?.message || 'Failed to send quote. Please try again.';
                this.cdr.detectChanges();
            }
        });
    }
}
