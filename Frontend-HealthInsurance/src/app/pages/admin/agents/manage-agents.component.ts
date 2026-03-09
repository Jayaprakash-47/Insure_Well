import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({ selector: 'app-manage-agents', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './manage-agents.component.html', styleUrl: './manage-agents.component.css' })
export class ManageAgentsComponent implements OnInit {
    agents: any[] = [];
    loading = true;
    showModal = false;
    creating = false;
    form = { firstName: '', lastName: '', email: '', password: '', phone: '', licenseNumber: '', territory: '', commissionPercentage: 10 };

    constructor(private api: ApiService, private toast: ToastService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void { this.loadAgents(); }

    loadAgents(): void {
        this.api.getAllAgents().subscribe({
            next: (data) => { this.agents = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    createAgent(): void {
        if (!this.form.firstName || !this.form.email || !this.form.password) { this.toast.error('Fill required fields'); return; }
        this.creating = true;
        this.api.createAgent(this.form).subscribe({
            next: () => { this.toast.success('Agent created successfully!'); this.showModal = false; this.creating = false; this.loadAgents(); this.resetForm(); this.cdr.detectChanges(); },
            error: (err) => { this.creating = false; this.toast.error(err.error?.message || 'Failed to create agent'); this.cdr.detectChanges(); }
        });
    }

    toggleStatus(agent: any): void {
        const action = agent.isActive ? this.api.deactivateUser(agent.userId) : this.api.activateUser(agent.userId);
        action.subscribe({
            next: () => { this.toast.success(agent.isActive ? 'Agent deactivated' : 'Agent activated'); this.loadAgents(); },
            error: () => this.toast.error('Action failed')
        });
    }

    resetForm(): void {
        this.form = { firstName: '', lastName: '', email: '', password: '', phone: '', licenseNumber: '', territory: '', commissionPercentage: 10 };
    }
}
