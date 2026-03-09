import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({ selector: 'app-agent-dashboard', standalone: true, imports: [CommonModule], templateUrl: './agent-dashboard.component.html', styleUrl: './agent-dashboard.component.css' })
export class AgentDashboardComponent implements OnInit {
    dashboard: any = null; loading = true;
    constructor(private api: ApiService, public auth: AuthService, private cdr: ChangeDetectorRef) { }
    ngOnInit(): void { this.api.getAgentDashboard().subscribe({ next: (d) => { this.dashboard = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } }); }
    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
}
