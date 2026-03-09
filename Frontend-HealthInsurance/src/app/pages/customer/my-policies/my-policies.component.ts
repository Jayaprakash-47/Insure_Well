import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({ selector: 'app-my-policies', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './my-policies.component.html', styleUrl: './my-policies.component.css' })
export class MyPoliciesComponent implements OnInit {
    policies: PolicyResponse[] = [];
    loading = true;
    constructor(private api: ApiService, private toast: ToastService, private cdr: ChangeDetectorRef) { }
    ngOnInit(): void { this.loadPolicies(); }

    loadPolicies(): void {
        this.api.getMyPolicies().subscribe({ next: (d) => { this.policies = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } });
    }

    cancel(id: number): void {
        if (!confirm('Cancel this policy?')) return;
        this.api.cancelPolicy(id).subscribe({ next: () => { this.toast.success('Policy cancelled'); this.loadPolicies(); }, error: (err) => this.toast.error(err.error?.message || 'Failed') });
    }

    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
}
