import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { InsurancePlan } from '../../../core/models/models';
@Component({ selector: 'app-agent-plans', standalone: true, imports: [CommonModule], templateUrl: './agent-plans.component.html', styleUrl: './agent-plans.component.css' })
export class AgentPlansComponent implements OnInit {
    plans: InsurancePlan[] = []; loading = true;
    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }
    ngOnInit(): void { this.api.getAgentPlans().subscribe({ next: (d) => { this.plans = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } }); }
    formatCurrency(n: number): string { return '₹' + n.toLocaleString('en-IN'); }
}
