import { Component, OnInit } from '@angular/core';

import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { CreateUnderwriterRequest } from '../../../core/models/models';

@Component({
  selector: 'app-manage-underwriters',
  standalone: true,
  imports: [RouterModule, FormsModule],
  templateUrl: './manage-underwriters.component.html',
  styleUrl: './manage-underwriters.component.css',
})
export class ManageUnderwritersComponent implements OnInit {
  underwriters: any[] = [];
  loading = true;
  showCreateForm = false;
  creating = false;
  createSuccess = false;
  createError = '';
  form: CreateUnderwriterRequest = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    phone: '',
    licenseNumber: '',
    specialization: '',
    commissionPercentage: 10,
  };
  errors: any = {};
  touched: any = {};

  constructor(
    private api: ApiService,
    private toast: ToastService,
  ) {}

  ngOnInit() {
    this.loadUnderwriters();
  }

  openCreateForm(): void {
    this.form = {
      firstName: '',
      lastName: '',
      email: '',
      password: '',
      phone: '',
      licenseNumber: '',
      specialization: '',
      commissionPercentage: 10,
    };
    this.errors = {};
    this.touched = {};
    this.createError = '';
    this.createSuccess = false;
    this.showCreateForm = true;
  }

  loadUnderwriters() {
    this.loading = true;
    this.api.getAllUnderwriters().subscribe({
      next: (d) => {
        this.underwriters = d;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  onBlur(field: string): void {
    this.touched[field] = true;
    this.validateField(field);
  }

  validateField(field: string): void {
    switch (field) {
      case 'firstName':
        if (!this.form.firstName?.trim()) {
          this.errors.firstName = 'First name cannot be empty';
        } else {
          delete this.errors.firstName;
        }
        break;
      case 'lastName':
        if (!this.form.lastName?.trim()) {
          this.errors.lastName = 'Last name cannot be empty';
        } else {
          delete this.errors.lastName;
        }
        break;
      case 'email':
        if (!this.form.email?.trim()) {
          this.errors.email = 'Email cannot be empty';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
          this.errors.email = 'Invalid email format';
        } else {
          delete this.errors.email;
        }
        break;
      case 'password':
        if (!this.form.password) {
          this.errors.password = 'Password cannot be empty';
        } else {
          delete this.errors.password;
        }
        break;
      case 'licenseNumber':
        if (!this.form.licenseNumber?.trim()) {
          this.errors.licenseNumber = 'License number cannot be empty';
        } else {
          delete this.errors.licenseNumber;
        }
        break;
      case 'commissionPercentage':
        if (
          this.form.commissionPercentage == null ||
          this.form.commissionPercentage < 0 ||
          this.form.commissionPercentage > 100
        ) {
          this.errors.commissionPercentage = 'Commission must be between 0 and 100';
        } else {
          delete this.errors.commissionPercentage;
        }
        break;
    }
  }

  createUnderwriter() {
    this.errors = {};
    this.touched = {
      firstName: true,
      lastName: true,
      email: true,
      password: true,
      licenseNumber: true,
      commissionPercentage: true,
    };

    this.validateField('firstName');
    this.validateField('lastName');
    this.validateField('email');
    this.validateField('password');
    this.validateField('licenseNumber');
    this.validateField('commissionPercentage');

    if (Object.keys(this.errors).length > 0) {
      this.toast.error('Please fill all required fields correctly');
      setTimeout(() => {
        const firstError = document.querySelector('.form-control.error');
        if (firstError) {
          (firstError as HTMLElement).focus();
        }
      }, 100);
      return;
    }

    this.creating = true;
    this.createError = '';
    this.createSuccess = false;
    this.api.createUnderwriter(this.form).subscribe({
      next: () => {
        this.creating = false;
        this.createSuccess = true;
        this.form = {
          firstName: '',
          lastName: '',
          email: '',
          password: '',
          phone: '',
          licenseNumber: '',
          specialization: '',
          commissionPercentage: 10,
        };
        this.loadUnderwriters();
        setTimeout(() => {
          this.createSuccess = false;
          this.showCreateForm = false;
        }, 2000);
      },
      error: (err) => {
        this.creating = false;
        this.createError = err?.error?.message || 'Failed to create underwriter.';
      },
    });
  }

  toggleUser(userId: number, activate: boolean) {
    const call = activate ? this.api.activateUser(userId) : this.api.deactivateUser(userId);
    call.subscribe({
      next: () => {
        this.toast.success(activate ? 'Underwriter activated' : 'Underwriter deactivated');
        this.loadUnderwriters();
      },
      error: () => this.toast.error('Action failed'),
    });
  }
}
