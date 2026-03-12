import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { DashboardResponse, PolicyResponse, ClaimResponse } from '../../../core/models/models';

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [RouterLink, CommonModule],
    templateUrl: './admin-dashboard.component.html',
    styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
    stats: DashboardResponse | null = null;
    loading = true;
    recentPolicies: PolicyResponse[] = [];
    recentClaims: ClaimResponse[] = [];

    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getAdminDashboard().subscribe({
            next: (data) => { this.stats = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
        this.api.getAllPolicies().subscribe({
            next: (data) => { this.recentPolicies = data; this.cdr.detectChanges(); },
            error: () => {}
        });
        this.api.getAllClaims().subscribe({
            next: (data) => { this.recentClaims = data; this.cdr.detectChanges(); },
            error: () => {}
        });
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }

    getStatusClass(status: string): string {
        const map: Record<string, string> = {
            'ACTIVE': 'badge-active', 'PENDING': 'badge-pending', 'EXPIRED': 'badge-inactive',
            'CANCELLED': 'badge-cancelled', 'SUBMITTED': 'badge-submitted',
            'UNDER_REVIEW': 'badge-info', 'APPROVED': 'badge-approved',
            'REJECTED': 'badge-rejected', 'SETTLED': 'badge-success',
            'ASSIGNED': 'badge-info', 'QUOTE_SENT': 'badge-info'
        };
        return map[status] || 'badge-info';
    }


}
