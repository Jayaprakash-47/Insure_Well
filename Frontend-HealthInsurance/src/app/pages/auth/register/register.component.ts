import { Component, ChangeDetectorRef } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { PasswordStrengthComponent } from '../../../shared/password-strength/password-strength';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, RouterLink, PasswordStrengthComponent], // ← add PasswordStrengthComponent
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
})
export class RegisterComponent {
  form = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phone: '',
    dateOfBirth: '',
    gender: 'MALE',
    address: '',
    city: '',
    state: '',
    pincode: '',
  };
  loading = false;
  showPassword = false; // ← NEW
  errors: any = {};
  touched: any = {};

  // ← NEW: sets the max attribute on the date input (no future dates)
  today = new Date().toISOString().split('T')[0];

  constructor(
    private auth: AuthService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  onBlur(field: string): void {
    this.touched[field] = true;
    this.validateField(field);
  }

  validateField(field: string): void {
    switch (field) {
      case 'firstName':
        if (!this.form.firstName?.trim()) this.errors.firstName = 'First name cannot be empty';
        else delete this.errors.firstName;
        break;

      case 'lastName':
        if (!this.form.lastName?.trim()) this.errors.lastName = 'Last name cannot be empty';
        else delete this.errors.lastName;
        break;

      case 'email':
        if (!this.form.email?.trim()) this.errors.email = 'Email cannot be empty';
        else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email))
          this.errors.email = 'Invalid email format';
        else delete this.errors.email;
        break;

      case 'password':
        if (!this.form.password) {
          this.errors.password = 'Password cannot be empty';
        } else if (this.form.password.length < 8) {
          this.errors.password = 'Password must be at least 8 characters';
        } else if (!/[A-Z]/.test(this.form.password)) {
          this.errors.password = 'Password must contain at least one uppercase letter';
        } else if (!/[0-9]/.test(this.form.password)) {
          this.errors.password = 'Password must contain at least one number';
        } else if (!/[^A-Za-z0-9]/.test(this.form.password)) {
          this.errors.password = 'Password must contain at least one special character';
        } else {
          delete this.errors.password;
        }
        break;

      case 'dateOfBirth':
        if (!this.form.dateOfBirth) {
          this.errors.dateOfBirth = 'Date of birth cannot be empty';
        } else {
          const dob = new Date(this.form.dateOfBirth);
          const today = new Date();
          today.setHours(0, 0, 0, 0);

          if (dob > today) {
            this.errors.dateOfBirth = 'Date of birth cannot be a future date';
          } else {
            // Minimum age check (18 years)
            const minDate = new Date();
            minDate.setFullYear(minDate.getFullYear() - 18);
            if (dob > minDate) {
              this.errors.dateOfBirth = 'You must be at least 18 years old';
            } else {
              delete this.errors.dateOfBirth;
            }
          }
        }
        break;

      case 'phone':
        if (!this.form.phone?.trim()) this.errors.phone = 'Phone number cannot be empty';
        else if (!/^\d{10}$/.test(this.form.phone.trim()))
          this.errors.phone = 'Phone number must be 10 digits';
        else delete this.errors.phone;
        break;
    }
  }

  onSubmit(): void {
    this.errors = {};
    this.touched = {
      firstName: true,
      lastName: true,
      email: true,
      password: true,
      dateOfBirth: true,
      phone: true,
    };

    // Run all validations
    ['firstName', 'lastName', 'email', 'password', 'dateOfBirth', 'phone'].forEach((f) =>
      this.validateField(f),
    );

    if (Object.keys(this.errors).length > 0) {
      this.toast.error('Please fill in all required fields correctly');
      setTimeout(() => {
        const firstError = document.querySelector('.form-control.error');
        if (firstError) (firstError as HTMLElement).focus();
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
      },
    });
  }
}
