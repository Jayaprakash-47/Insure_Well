import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { InsurancePlan, PremiumQuoteResponse } from '../../../core/models/models';

@Component({ selector: 'app-browse-plans', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './browse-plans.component.html', styleUrl: './browse-plans.component.css' })
export class BrowsePlansComponent implements OnInit {
    plans: InsurancePlan[] = [];
    loading = true;
    showQuoteModal = false;
    showPurchaseModal = false;
    selectedPlan: InsurancePlan | null = null;
    quoteResult: PremiumQuoteResponse | null = null;
    quoting = false;
    purchasing = false;
    quoteForm = { age: 30, smoker: false, preExistingDiseases: false, numberOfMembers: 1 };
    purchaseForm = { nomineeName: '', nomineeRelationship: '', members: [] as any[] };

    constructor(private api: ApiService, private toast: ToastService, private router: Router, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getAllPlans().subscribe({ next: (d) => { this.plans = d.filter(p => p.isActive); this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } });
    }

    openQuote(plan: InsurancePlan): void { this.selectedPlan = plan; this.quoteResult = null; this.showQuoteModal = true; this.cdr.detectChanges(); }

    getQuote(): void {
        if (!this.selectedPlan) return;
        this.quoting = true; this.cdr.detectChanges();
        this.api.calculatePremium({ planId: this.selectedPlan.planId, ...this.quoteForm }).subscribe({
            next: (res) => { this.quoteResult = res; this.quoting = false; this.cdr.detectChanges(); },
            error: (err) => { this.quoting = false; this.toast.error(err.error?.message || 'Quote failed'); this.cdr.detectChanges(); }
        });
    }

    openPurchase(): void { this.showQuoteModal = false; this.showPurchaseModal = true; this.cdr.detectChanges(); }

    purchase(): void {
        if (!this.selectedPlan || !this.purchaseForm.nomineeName) { this.toast.error('Fill nominee details'); return; }
        this.purchasing = true; this.cdr.detectChanges();
        this.api.purchasePolicy({
            planId: this.selectedPlan.planId,
            quoteId: this.quoteResult?.quoteId,
            nomineeName: this.purchaseForm.nomineeName,
            nomineeRelationship: this.purchaseForm.nomineeRelationship,
            members: this.purchaseForm.members
        }).subscribe({
            next: (res) => { this.toast.success('Policy purchased! Pay to activate.'); this.showPurchaseModal = false; this.purchasing = false; this.router.navigate(['/customer/payment', res.policyId]); },
            error: (err) => { this.purchasing = false; this.toast.error(err.error?.message || 'Purchase failed'); this.cdr.detectChanges(); }
        });
    }

    formatCurrency(n: number): string { return '₹' + n.toLocaleString('en-IN'); }
}
