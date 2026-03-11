import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
    selector: 'app-send-quote',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './send-quote.component.html',
    styleUrl: './send-quote.component.css'
})
export class SendQuoteComponent implements OnInit {
    policy: PolicyResponse | null = null;
    loading = true;
    quoteAmount = 0;
    remarks = '';
    sending = false;
    success = false;
    error = '';
    commissionPct = 10; // default

    get estimatedCommission(): number {
        return Math.round(this.quoteAmount * (this.commissionPct / 100));
    }

    constructor(private api: ApiService, private route: ActivatedRoute, private router: Router) { }

    ngOnInit() {
        const policyId = this.route.snapshot.paramMap.get('policyId');
        if (policyId) {
            this.api.getPolicyById(+policyId).subscribe({
                next: (p) => { this.policy = p; this.loading = false; },
                error: () => { this.loading = false; }
            });
        } else {
            this.loading = false;
        }
    }

    submitQuote() {
        if (!this.policy || !this.quoteAmount) return;
        this.sending = true; this.error = '';
        const req = { quoteAmount: this.quoteAmount, remarks: this.remarks };
        this.api.sendQuote(this.policy.policyId, req).subscribe({
            next: () => {
                this.sending = false; this.success = true;
                setTimeout(() => this.router.navigate(['/underwriter/pending']), 2000);
            },
            error: (err) => {
                this.sending = false;
                this.error = err?.error?.message || 'Failed to send quote.';
            }
        });
    }
}
