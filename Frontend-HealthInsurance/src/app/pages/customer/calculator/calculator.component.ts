import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { InsurancePlan, PremiumQuoteResponse } from '../../../core/models/models';

@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './calculator.component.html',
  styleUrl: './calculator.component.css',
})
export class CalculatorComponent implements OnInit {
  plans: InsurancePlan[] = [];
  quotes: PremiumQuoteResponse[] = [];
  loading = true;
  calculating = false;
  form = { planId: 0, age: 30, smoker: false, preExistingDiseases: false, numberOfMembers: 1 };
  result: PremiumQuoteResponse | null = null;

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.api.getAllPlans().subscribe({
      next: (d) => {
        this.plans = d.filter((p) => p.isActive);
        this.loading = false;
        if (this.plans.length) this.form.planId = this.plans[0].planId;
        this.cdr.detectChanges();
      },
    });
    this.api.getMyQuotes().subscribe({
      next: (d) => {
        this.quotes = d;
        this.cdr.detectChanges();
      },
    });
  }

  calculate(): void {
    if (!this.form.planId) {
      this.toast.error('Select a plan');
      return;
    }
    this.calculating = true;
    this.cdr.detectChanges();
    this.api.calculatePremium(this.form).subscribe({
      next: (res) => {
        this.result = res;
        this.calculating = false;
        this.quotes.unshift(res);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.calculating = false;
        this.toast.error(err.error?.message || 'Calculation failed');
        this.cdr.detectChanges();
      },
    });
  }

  formatCurrency(n: number): string {
    return '₹' + n.toLocaleString('en-IN');
  }
}
