import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

declare var Razorpay: any; // ← tells TypeScript Razorpay exists globally

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css',
})
export class PaymentComponent implements OnInit {
  policy: PolicyResponse | null = null;
  loading = true;
  paying = false;
  paymentMethod = 'UPI';
  razorpayLoaded = false;

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private route: ActivatedRoute,
    public router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('policyId'));
    this.api.getPolicyById(id).subscribe({
      next: (d) => {
        this.policy = d;
        this.loading = false;
        this.loadRazorpayScript(); // ← preload script
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  // ── Load Razorpay checkout script ──
  private loadRazorpayScript(): void {
    if (document.getElementById('razorpay-script')) {
      this.razorpayLoaded = true;
      return;
    }
    const script = document.createElement('script');
    script.id = 'razorpay-script';
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => {
      this.razorpayLoaded = true;
      this.cdr.detectChanges();
    };
    document.body.appendChild(script);
  }

  // ── Main pay() method ──
  pay(): void {
    if (!this.policy) return;
    this.paying = true;
    this.cdr.detectChanges();

    // Step 1: Create order on backend
    this.api.createPaymentOrder(this.policy.policyId).subscribe({
      next: (order: any) => {
        this.openRazorpay(order);
      },
      error: (err: any) => {
        this.paying = false;
        // Show specific backend error messages
        const backendError =
          err?.error?.error || err?.error?.message || err?.message || 'Failed to initiate payment';
        this.toast.error(backendError);
        this.cdr.detectChanges();
      },
    });
  }

  // ── Open Razorpay checkout modal ──
  private openRazorpay(order: any): void {
    const options = {
      key: order.keyId,
      amount: order.amount,
      currency: order.currency,
      name: 'InsureWell Health Insurance',
      description: 'Premium Payment — ' + order.policyNumber,
      image: 'https://i.imgur.com/3g7nmJC.png', // optional logo
      order_id: order.orderId,

      // Pre-fill customer details
      prefill: {
        name: order.customerName,
        email: order.customerEmail,
        contact: order.customerPhone || '',
      },

      notes: {
        policyId: order.policyId,
        policyNumber: order.policyNumber,
      },

      theme: {
        color: '#800020', // ← your brand color
      },

      // ── Payment method config ──
      config: {
        display: {
          blocks: {
            utib: { name: 'Pay using UPI', instruments: [{ method: 'upi' }] },
            other: {
              name: 'Other Methods',
              instruments: [{ method: 'card' }, { method: 'netbanking' }, { method: 'wallet' }],
            },
          },
          sequence: ['block.utib', 'block.other'],
          preferences: {
            show_default_blocks: true,
          },
        },
      },

      // ── Success handler ──
      handler: (response: any) => {
        this.verifyPayment(order, response);
      },

      // ── Modal dismissed (user closed) ──
      modal: {
        ondismiss: () => {
          this.paying = false;
          this.toast.error('Payment cancelled');
          this.cdr.detectChanges();
        },
      },
    };

    const rzp = new Razorpay(options);

    // ── Payment failure handler ──
    rzp.on('payment.failed', (response: any) => {
      this.paying = false;
      this.toast.error('Payment failed: ' + response.error.description);

      // Notify backend of failure
      this.api
        .recordPaymentFailure({
          policyId: order.policyId.toString(),
          errorCode: response.error.code,
          errorDescription: response.error.description,
        })
        .subscribe();

      this.cdr.detectChanges();
    });

    rzp.open();
  }

  // ── Verify payment on backend ──
  private verifyPayment(order: any, response: any): void {
    this.api
      .verifyPayment({
        policyId: order.policyId.toString(),
        razorpayOrderId: order.orderId,
        razorpayPaymentId: response.razorpay_payment_id,
        razorpaySignature: response.razorpay_signature,
      })
      .subscribe({
        next: (res: any) => {
          this.paying = false;
          this.toast.success('🎉 Payment successful! Your policy is now ACTIVE.');
          this.router.navigate(['/customer/policies']);
        },
        error: (err: any) => {
          this.paying = false;
          this.toast.error(err?.error?.error || 'Payment verification failed');
          this.cdr.detectChanges();
        },
      });
  }

  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }
}
