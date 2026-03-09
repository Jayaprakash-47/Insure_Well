import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { DashboardResponse } from '../../../core/models/models';

@Component({
    selector: 'app-admin-dashboard',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './admin-dashboard.component.html',
    styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {
    stats: DashboardResponse | null = null;
    loading = true;

    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getAdminDashboard().subscribe({
            next: (data) => { this.stats = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    formatCurrency(amount: number): string {
        return '₹' + (amount || 0).toLocaleString('en-IN');
    }
}
