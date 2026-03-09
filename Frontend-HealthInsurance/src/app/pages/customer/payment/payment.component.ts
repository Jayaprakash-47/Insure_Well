import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({ selector: 'app-payment', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './payment.component.html', styleUrl: './payment.component.css' })
export class PaymentComponent implements OnInit {
    policy: PolicyResponse | null = null;
    loading = true;
    paying = false;
    paymentMethod = 'UPI';

    constructor(private api: ApiService, private toast: ToastService, private route: ActivatedRoute, private router: Router, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        const id = Number(this.route.snapshot.paramMap.get('policyId'));
        this.api.getPolicyById(id).subscribe({ next: (d) => { this.policy = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } });
    }

    pay(): void {
        if (!this.policy) return;
        this.paying = true; this.cdr.detectChanges();
        this.api.makePayment({ policyId: this.policy.policyId, amount: this.policy.premiumAmount, paymentMethod: this.paymentMethod }).subscribe({
            next: () => { this.toast.success('Payment successful! Policy is now ACTIVE'); this.router.navigate(['/customer/policies']); },
            error: (err) => { this.paying = false; this.toast.error(err.error?.message || 'Payment failed'); this.cdr.detectChanges(); }
        });
    }

    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
}
