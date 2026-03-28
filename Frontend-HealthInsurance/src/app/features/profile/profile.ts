// FILE: src/app/features/profile/profile.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';

interface ProfileData {
  userId: number;
  name: string;
  email: string;
  phone: string;
  role: string;
  address: string;
  city: string;
  state: string;
  pincode: string;
  createdAt: string;
  accountNumber?: string;
  ifscCode?: string;
  accountHolderName?: string;
  bankName?: string;
  aadhaarNumber?: string;
  aadhaarVerified?: boolean;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.css']
})
export class Profile implements OnInit {

  activeTab: 'info' | 'password' | 'bank' = 'info';
  profile: ProfileData | null = null;
  editMode = false;
  bankEditMode = false; // Separate edit mode for Bank Details tab
  saving = false;
  changingPw = false;

  showCurrent = false;
  showNew = false;
  showConfirm = false;

  editData = {
    name: '', phone: '', address: '', city: '', state: '', pincode: '',
    accountNumber: '', ifscCode: '', accountHolderName: '', bankName: ''
  };

  pwData = {
    currentPassword: '', newPassword: '', confirmPassword: ''
  };

  constructor(
    private api: ApiService,
    private auth: AuthService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.api.getProfile().subscribe({
      next: (data: ProfileData) => { 
        this.profile = data; 
        this.populateEditData();
      },
      error: () => { this.toast.error('Failed to load profile'); }
    });
  }

  get hasBankDetails(): boolean {
    return !!(this.profile?.accountNumber && this.profile?.ifscCode && this.profile?.bankName);
  }

  enterEditMode(): void {
    this.populateEditData();
    this.editMode = true;
  }

  cancelEdit(): void {
    this.editMode = false;
  }

  enterBankEditMode(): void {
    this.populateEditData();
    this.bankEditMode = true;
  }

  cancelBankEdit(): void {
    this.bankEditMode = false;
  }

  private populateEditData(): void {
    this.editData = {
      name:    this.profile?.name    || '',
      phone:   this.profile?.phone   || '',
      address: this.profile?.address || '',
      city:    this.profile?.city    || '',
      state:   this.profile?.state   || '',
      pincode: this.profile?.pincode || '',
      accountNumber: this.profile?.accountNumber || '',
      ifscCode: this.profile?.ifscCode || '',
      accountHolderName: this.profile?.accountHolderName || '',
      bankName: this.profile?.bankName || ''
    };
  }

  saveProfile(form: NgForm, isBankEdit: boolean = false): void {
    if (form.invalid) return;
    this.saving = true;
    this.api.updateProfile(this.editData).subscribe({
      next: (updated: ProfileData) => {
        this.profile = updated;
        this.populateEditData();
        if (isBankEdit) {
          this.bankEditMode = false;
          this.toast.success(this.hasBankDetails ? 'Bank details updated successfully' : 'Bank details added successfully');
        } else {
          this.editMode = false;
          this.toast.success('Profile updated successfully');
        }
        this.saving = false;
      },
      error: (err: any) => {
        this.saving = false;
        let errMsg = 'Update failed';
        if (err?.error?.message) {
           if (typeof err.error.message === 'object') {
              errMsg = Object.values(err.error.message).join(', ');
           } else {
              errMsg = err.error.message;
           }
        }
        this.toast.error(errMsg);
      }
    });
  }

  submitPasswordChange(form: NgForm): void {
    if (form.invalid || this.pwData.newPassword !== this.pwData.confirmPassword) return;
    this.changingPw = true;
    this.api.changePassword(this.pwData).subscribe({
      next: () => {
        this.changingPw = false;
        form.resetForm();
        this.pwData = { currentPassword: '', newPassword: '', confirmPassword: '' };
        this.toast.success('Password updated! Please log in again.');
        setTimeout(() => this.auth.logout(), 2000);
      },
      error: (err: any) => {
        this.changingPw = false;
        this.toast.error(err?.error?.message || 'Failed to change password');
      }
    });
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  getInitials(): string {
    if (!this.profile?.name) return '?';
    return this.profile.name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map(w => w[0].toUpperCase())
      .join('');
  }

  formatRole(role?: string): string {
    if (!role) return '';
    const map: Record<string, string> = {
      ADMIN: 'Administrator',
      UNDERWRITER: 'Underwriter',
      CLAIMS_OFFICER: 'Claims Officer',
      CUSTOMER: 'Customer'
    };
    return map[role] || role;
  }

  getStrengthScore(): number {
    const pw = this.pwData.newPassword;
    if (!pw) return 0;
    let score = 0;
    if (pw.length >= 8)      score++;
    if (pw.length >= 12)     score++;
    if (this.hasUppercase()) score++;
    if (this.hasNumber())    score++;
    if (this.hasSpecial())   score++;
    return score;
  }

  getStrengthPercent(): number {
    return (this.getStrengthScore() / 5) * 100;
  }

  getStrengthLevel(): string {
    const s = this.getStrengthScore();
    if (s <= 1) return 'weak';
    if (s <= 2) return 'fair';
    if (s <= 3) return 'good';
    return 'strong';
  }

  getStrengthLabel(): string {
    const map: Record<string, string> = {
      weak: 'Weak', fair: 'Fair', good: 'Good', strong: 'Strong'
    };
    return map[this.getStrengthLevel()];
  }

  hasUppercase(): boolean { return /[A-Z]/.test(this.pwData.newPassword); }
  hasNumber(): boolean    { return /[0-9]/.test(this.pwData.newPassword); }
  hasSpecial(): boolean   { return /[^A-Za-z0-9]/.test(this.pwData.newPassword); }
}
