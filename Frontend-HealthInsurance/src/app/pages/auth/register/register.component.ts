import { Component, ChangeDetectorRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [FormsModule, RouterLink],
    templateUrl: './register.component.html',
    styleUrl: './register.component.css'
})
export class RegisterComponent {
    form = {
        firstName: '', lastName: '', email: '', password: '',
        phone: '', dateOfBirth: '', gender: 'MALE',
        address: '', city: '', state: '', pincode: ''
    };
    loading = false;
    errors: any = {};
    touched: any = {};

    constructor(
        private auth: AuthService,
        private toast: ToastService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    onBlur(field: string): void {
        this.touched[field] = true;
        this.validateField(field);
    }

    validateField(field: string): void {
        switch (field) {
            case 'firstName':
                if (!this.form.firstName?.trim()) {
                    this.errors.firstName = 'First name cannot be empty';
                } else { delete this.errors.firstName; }
                break;
            case 'lastName':
                if (!this.form.lastName?.trim()) {
                    this.errors.lastName = 'Last name cannot be empty';
                } else { delete this.errors.lastName; }
                break;
            case 'email':
                if (!this.form.email?.trim()) {
                    this.errors.email = 'Email cannot be empty';
                } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
                    this.errors.email = 'Invalid email format';
                } else { delete this.errors.email; }
                break;
            case 'password':
                if (!this.form.password || this.form.password.length < 6) {
                    this.errors.password = 'Password must be at least 6 characters';
                } else { delete this.errors.password; }
                break;
            case 'dateOfBirth':
                if (!this.form.dateOfBirth) {
                    this.errors.dateOfBirth = 'Date of birth cannot be empty';
                } else { delete this.errors.dateOfBirth; }
                break;
            case 'phone':
                if (!this.form.phone?.trim()) {
                    this.errors.phone = 'Phone number cannot be empty';
                } else { delete this.errors.phone; }
                break;
        }
    }

    onSubmit(): void {
        this.errors = {};
        this.touched = { firstName: true, lastName: true, email: true, password: true, dateOfBirth: true, phone: true };
        let isValid = true;

        if (!this.form.firstName?.trim()) {
            this.errors.firstName = 'First name cannot be empty';
            isValid = false;
        }
        if (!this.form.lastName?.trim()) {
            this.errors.lastName = 'Last name cannot be empty';
            isValid = false;
        }
        if (!this.form.email?.trim()) {
            this.errors.email = 'Email cannot be empty';
            isValid = false;
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
            this.errors.email = 'Invalid email format';
            isValid = false;
        }
        if (!this.form.password || this.form.password.length < 6) {
            this.errors.password = 'Password must be at least 6 characters';
            isValid = false;
        }
        if (!this.form.dateOfBirth) {
            this.errors.dateOfBirth = 'Date of birth cannot be empty';
            isValid = false;
        }
        if (!this.form.phone?.trim()) {
            this.errors.phone = 'Phone number cannot be empty';
            isValid = false;
        }

        if (!isValid) {
            this.toast.error('Please fill in all required fields correctly');
            setTimeout(() => {
                const firstError = document.querySelector('.form-control.error');
                if (firstError) {
                    (firstError as HTMLElement).focus();
                }
            }, 100);
            return;
        }

        this.loading = true;
        this.cdr.detectChanges();
        this.auth.register(this.form).subscribe({
            next: () => {
                this.toast.success('Account created successfully!');
                this.router.navigate(['/customer/dashboard']);
            },
            error: (err) => {
                this.loading = false;
                this.toast.error(err.error?.message || 'Registration failed');
                this.cdr.detectChanges();
            }
        });
    }
}
