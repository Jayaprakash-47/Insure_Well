import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ClaimsOfficerDashboardResponse } from '../../../core/models/models';

@Component({
    selector: 'app-co-dashboard',
    standalone: true,
    imports: [RouterLink],
    templateUrl: './co-dashboard.component.html',
    styleUrl: './co-dashboard.component.css'
})
export class CODashboardComponent implements OnInit {
    dashboard: ClaimsOfficerDashboardResponse | null = null;
    loading = true;

    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getOfficerDashboard().subscribe({
            next: (data) => { this.dashboard = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }
}
