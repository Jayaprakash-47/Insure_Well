import { Component, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
    selector: 'app-register',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterLink],
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

    constructor(
        private auth: AuthService,
        private toast: ToastService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    onSubmit(): void {
        if (!this.form.firstName || !this.form.email || !this.form.password || !this.form.dateOfBirth) {
            this.toast.error('Please fill in all required fields');
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
