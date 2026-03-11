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

    constructor(
        private auth: AuthService,
        private toast: ToastService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    onSubmit(): void {
        if (!this.email || !this.password) {
            this.toast.error('Please fill in all fields');
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
