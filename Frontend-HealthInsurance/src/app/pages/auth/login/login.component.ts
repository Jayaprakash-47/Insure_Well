import { Component, ChangeDetectorRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [FormsModule, RouterLink],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css'
})
export class LoginComponent {
    email = '';
    password = '';
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
        if (field === 'email') {
            if (!this.email?.trim()) {
                this.errors.email = 'Email cannot be empty';
            } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.email)) {
                this.errors.email = 'Invalid email format';
            } else {
                delete this.errors.email;
            }
        }
        if (field === 'password') {
            if (!this.password) {
                this.errors.password = 'Password cannot be empty';
            } else {
                delete this.errors.password;
            }
        }
    }

    onSubmit(): void {
        this.errors = {};
        this.touched = { email: true, password: true };

        this.validateField('email');
        this.validateField('password');

        if (Object.keys(this.errors).length > 0) {
            this.toast.error('Please fill in all fields correctly');
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
        this.auth.login({ email: this.email, password: this.password }).subscribe({
            next: () => {
                this.toast.success('Login successful!');
                this.router.navigate([this.auth.getDashboardRoute()]);
            },
            error: (err) => {
                this.loading = false;
                const msg = err.error?.message || 'Invalid credentials';
                this.toast.error(msg);
                this.cdr.detectChanges();
            }
        });
    }
}
