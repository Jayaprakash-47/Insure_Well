import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { InsurancePlan } from '../../core/models/models';

@Component({
  selector: 'app-plans',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './plans.component.html',
  styleUrl: './plans.component.css',
})
export class PlansComponent implements OnInit {
  plans: InsurancePlan[] = [];
  loading = true;
  searchTerm = '';
  selectedPlanType = 'ALL';
  planTypes: string[] = [];

  constructor(
    private api: ApiService,
    public auth: AuthService,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.api.getAllPlans().subscribe({
      next: (data) => {
        this.plans = data.filter((p) => p.isActive);
        this.planTypes = [...new Set(this.plans.map((p) => p.planType))];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  get filteredPlans(): InsurancePlan[] {
    return this.plans.filter((p) => {
      const matchesSearch =
        !this.searchTerm ||
        p.planName.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        p.description.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesType = this.selectedPlanType === 'ALL' || p.planType === this.selectedPlanType;
      return matchesSearch && matchesType;
    });
  }

  setFilter(type: string): void {
    this.selectedPlanType = type;
  }

  formatCurrency(amount: number): string {
    return '₹' + amount.toLocaleString('en-IN');
  }
}
