import { Component, OnInit } from '@angular/core';

import { RouterModule, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { CreateUnderwriterRequest } from '../../../core/models/models';

@Component({
    selector: 'app-manage-underwriters',
    standalone: true,
    imports: [RouterModule, FormsModule],
    templateUrl: './manage-underwriters.component.html',
    styleUrl: './manage-underwriters.component.css'
})
export class ManageUnderwritersComponent implements OnInit {
    underwriters: any[] = [];
    loading = true;
    showCreateForm = false;
    creating = false;
    createSuccess = false;
    createError = '';
    form: CreateUnderwriterRequest = { firstName: '', lastName: '', email: '', password: '', phone: '', licenseNumber: '', specialization: '', commissionPercentage: 10 };

    constructor(private api: ApiService, private toast: ToastService) { }

    ngOnInit() {
        this.loadUnderwriters();
    }

    loadUnderwriters() {
        this.loading = true;
        this.api.getAllUnderwriters().subscribe({
            next: (d) => { this.underwriters = d; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    createUnderwriter() {
        this.creating = true; this.createError = ''; this.createSuccess = false;
        this.api.createUnderwriter(this.form).subscribe({
            next: () => {
                this.creating = false; this.createSuccess = true;
                this.form = { firstName: '', lastName: '', email: '', password: '', phone: '', licenseNumber: '', specialization: '', commissionPercentage: 10 };
                this.loadUnderwriters();
                setTimeout(() => { this.createSuccess = false; this.showCreateForm = false; }, 2000);
            },
            error: (err) => {
                this.creating = false;
                this.createError = err?.error?.message || 'Failed to create underwriter.';
            }
        });
    }

    toggleUser(userId: number, activate: boolean) {
        const call = activate ? this.api.activateUser(userId) : this.api.deactivateUser(userId);
        call.subscribe({
            next: () => {
                this.toast.success(activate ? 'Underwriter activated' : 'Underwriter deactivated');
                this.loadUnderwriters();
            },
            error: () => this.toast.error('Action failed')
        });
    }
}
