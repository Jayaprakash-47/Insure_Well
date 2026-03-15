import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';
import { PasswordStrengthComponent } from '../../../../shared/password-strength/password-strength';
@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [FormsModule, RouterLink, PasswordStrengthComponent], // ← CommonModule removed
  templateUrl: './forgot-password.html',
})
export class ForgotPasswordComponent {
  step = 1;
  email = '';
  otp = '';
  newPassword = '';
  confirmPassword = '';
  showPassword = false;
  loading = false;
  message = '';
  isError = false;

  constructor(
    private auth: AuthService,
    private router: Router,
  ) {}

  sendOtp(): void {
    if (!this.email) {
      this.showError('Please enter your email');
      return;
    }
    this.loading = true;
    this.auth.sendForgotPasswordOtp(this.email).subscribe({
      next: () => {
        this.step = 2;
        this.loading = false;
        this.clearMessage();
      },
      error: (e) => {
        this.showError(e.error?.error || 'Email not found');
        this.loading = false;
      },
    });
  }

  verifyOtp(): void {
    if (this.otp.length !== 6) {
      this.showError('Enter a valid 6-digit OTP');
      return;
    }
    this.step = 3;
    this.clearMessage();
  }

  resetPassword(): void {
    if (this.newPassword !== this.confirmPassword) {
      this.showError('Passwords do not match');
      return;
    }
    if (this.newPassword.length < 8) {
      this.showError('Password must be at least 8 characters');
      return;
    }
    this.loading = true;
    this.auth.resetPassword(this.email, this.otp, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.message = 'Password reset successful! Redirecting to login...';
        this.isError = false;
        setTimeout(() => this.router.navigate(['/login']), 2500);
      },
      error: (e) => {
        this.showError(e.error?.error || 'Reset failed. Please try again.');
        this.loading = false;
      },
    });
  }

  private showError(msg: string): void {
    this.message = msg;
    this.isError = true;
  }
  private clearMessage(): void {
    this.message = '';
    this.isError = false;
  }
}
