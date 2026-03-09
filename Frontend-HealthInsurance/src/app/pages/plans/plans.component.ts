import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { InsurancePlan } from '../../core/models/models';

@Component({
    selector: 'app-plans',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './plans.component.html',
    styleUrl: './plans.component.css'
})
export class PlansComponent implements OnInit {
    plans: InsurancePlan[] = [];
    loading = true;

    constructor(private api: ApiService, public auth: AuthService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getAllPlans().subscribe({
            next: (data) => { this.plans = data.filter(p => p.isActive); this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    formatCurrency(amount: number): string {
        return '₹' + amount.toLocaleString('en-IN');
    }
}
