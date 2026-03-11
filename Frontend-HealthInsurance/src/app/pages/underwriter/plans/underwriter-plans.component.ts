import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { InsurancePlan } from '../../../core/models/models';

@Component({
    selector: 'app-underwriter-plans',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './underwriter-plans.component.html',
    styleUrl: './underwriter-plans.component.css'
})
export class UnderwriterPlansComponent implements OnInit {
    plans: InsurancePlan[] = [];
    loading = true;

    constructor(private api: ApiService) { }

    ngOnInit() {
        this.api.getAllPlans().subscribe({
            next: (d) => { this.plans = d; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    formatCoverage(amount?: number): string {
        if (!amount) return '0';
        if (amount >= 100000) return (amount / 100000).toFixed(1) + ' L';
        return amount.toLocaleString();
    }
}
