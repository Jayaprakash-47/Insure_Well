import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';
import { UnderwriterDashboardResponse } from '../../../core/models/models';

@Component({
    selector: 'app-underwriter-dashboard',
    standalone: true,
    imports: [RouterModule],
    templateUrl: './underwriter-dashboard.component.html',
    styleUrl: './underwriter-dashboard.component.css'
})
export class UnderwriterDashboardComponent implements OnInit {
    profile: UnderwriterDashboardResponse | null = null;
    userName = '';

    constructor(private api: ApiService, private auth: AuthService, private cdr: ChangeDetectorRef) { }

    ngOnInit() {
        this.userName = this.auth.getUserName();
        this.api.getUnderwriterDashboard().subscribe({
            next: (data) => { this.profile = data; this.cdr.detectChanges(); },
            error: (err) => console.error('Failed to load dashboard', err)
        });
    }

    formatAmount(val?: number): string {
        if (!val) return '₹0';
        if (val >= 100000) return '₹' + (val / 100000).toFixed(1) + 'L';
        if (val >= 1000) return '₹' + (val / 1000).toFixed(1) + 'K';
        return '₹' + val;
    }
}
