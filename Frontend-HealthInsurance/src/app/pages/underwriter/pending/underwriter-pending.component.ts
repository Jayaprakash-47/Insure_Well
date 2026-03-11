import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
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

    constructor(private api: ApiService, private router: Router, private cdr: ChangeDetectorRef) { }

    ngOnInit() {
        this.api.getUnderwriterPendingAssignments().subscribe({
            next: (data) => { this.policies = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
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
