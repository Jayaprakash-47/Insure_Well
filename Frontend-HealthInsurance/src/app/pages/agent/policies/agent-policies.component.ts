import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { PolicyResponse } from '../../../core/models/models';
@Component({ selector: 'app-agent-policies', standalone: true, imports: [CommonModule], templateUrl: './agent-policies.component.html', styleUrl: './agent-policies.component.css' })
export class AgentPoliciesComponent implements OnInit {
    policies: PolicyResponse[] = []; loading = true;
    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }
    ngOnInit(): void { this.api.getAgentPolicies().subscribe({ next: (d) => { this.policies = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } }); }
    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
}
