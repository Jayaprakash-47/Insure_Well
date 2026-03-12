import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { CreateClaimsOfficerRequest } from '../../../core/models/models';

@Component({
    selector: 'app-manage-claims-officers',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './manage-claims-officers.component.html',
    styleUrl: './manage-claims-officers.component.css'
})
export class ManageClaimsOfficersComponent implements OnInit {
    officers: any[] = [];
    loading = true;
    showModal = false;
    submitting = false;
    createSuccess = false;
    createError = '';

    form: CreateClaimsOfficerRequest = {
        firstName: '', lastName: '', email: '', password: '',
        phone: '', employeeId: '', department: '', approvalLimit: 500000
    };
    errors: any = {};
    touched: any = {};

    constructor(private api: ApiService, private toast: ToastService) { }

    ngOnInit(): void { this.loadOfficers(); }

    loadOfficers(): void {
        this.loading = true;
        this.api.getAllClaimsOfficers().subscribe({
            next: (data) => { this.officers = data; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    openCreate(): void {
        this.form = { firstName: '', lastName: '', email: '', password: '', phone: '', employeeId: '', department: 'Claims Processing', approvalLimit: 500000 };
        this.errors = {};
        this.touched = {};
        this.createSuccess = false;
        this.createError = '';
        this.showModal = true;
    }

    onBlur(field: string): void {
        this.touched[field] = true;
        this.validateField(field);
    }

    validateField(field: string): void {
        switch (field) {
            case 'firstName':
                if (!this.form.firstName?.trim()) { this.errors.firstName = 'First name cannot be empty'; } else { delete this.errors.firstName; }
                break;
            case 'lastName':
                if (!this.form.lastName?.trim()) { this.errors.lastName = 'Last name cannot be empty'; } else { delete this.errors.lastName; }
                break;
            case 'email':
                if (!this.form.email?.trim()) { this.errors.email = 'Email cannot be empty'; }
                else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) { this.errors.email = 'Invalid email format'; }
                else { delete this.errors.email; }
                break;
            case 'password':
                if (!this.form.password) { this.errors.password = 'Password cannot be empty'; } else { delete this.errors.password; }
                break;
        }
    }

    create(): void {
        this.errors = {};
        this.touched = { firstName: true, lastName: true, email: true, password: true };

        this.validateField('firstName');
        this.validateField('lastName');
        this.validateField('email');
        this.validateField('password');

        if (Object.keys(this.errors).length > 0) {
            this.toast.error('Please fill all required fields correctly');
            setTimeout(() => {
                const firstError = document.querySelector('.form-control.error');
                if (firstError) { (firstError as HTMLElement).focus(); }
            }, 100);
            return;
        }

        this.submitting = true;
        this.createSuccess = false;
        this.createError = '';
        this.api.createClaimsOfficer(this.form).subscribe({
            next: () => {
                this.createSuccess = true;
                this.toast.success('Claims Officer created');
                this.loadOfficers();
                this.submitting = false;
                setTimeout(() => { this.showModal = false; this.createSuccess = false; }, 1500);
            },
            error: (err) => {
                this.createError = err.error?.message || 'Failed to create officer';
                this.toast.error(this.createError);
                this.submitting = false;
            }
        });
    }

    toggleStatus(officer: any): void {
        const action = officer.isActive ? this.api.deactivateUser(officer.userId) : this.api.activateUser(officer.userId);
        action.subscribe({
            next: () => {
                this.toast.success(officer.isActive ? 'Claims Officer deactivated' : 'Claims Officer activated');
                this.loadOfficers();
            },
            error: () => { this.toast.error('Action failed'); }
        });
    }
}
