import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({ selector: 'app-manage-customers', standalone: true, imports: [], templateUrl: './manage-customers.component.html', styleUrl: './manage-customers.component.css' })
export class ManageCustomersComponent implements OnInit {
    customers: any[] = [];
    loading = true;

    constructor(private api: ApiService, private toast: ToastService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.loadCustomers();
    }

    loadCustomers(): void {
        this.api.getAllCustomers().subscribe({
            next: (data) => { this.customers = data; this.loading = false; this.cdr.detectChanges(); },
            error: () => { this.loading = false; this.cdr.detectChanges(); }
        });
    }

    toggleStatus(customer: any): void {
        const action = customer.isActive
            ? this.api.deactivateUser(customer.userId)
            : this.api.activateUser(customer.userId);

        action.subscribe({
            next: () => {
                this.toast.success(customer.isActive ? 'Customer deactivated' : 'Customer activated');
                this.loadCustomers();
            },
            error: () => this.toast.error('Action failed')
        });
    }
}
