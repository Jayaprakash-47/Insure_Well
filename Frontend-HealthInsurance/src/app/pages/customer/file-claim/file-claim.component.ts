// FILE: file-claim.component.ts
import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-file-claim',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './file-claim.component.html',
  styleUrl: './file-claim.component.css',
})
export class FileClaimComponent implements OnInit {
  policies: PolicyResponse[] = [];
  filing = false;
  selectedFiles: File[] = [];
  bankDetailsFilled = true; // Default to true while loading
  // Today in local timezone — used as [max] for admission date only
  today = this.toLocalDateString(new Date());

  form = {
    policyId: 0,
    claimType: 'CASHLESS',
    claimAmount: 0,
    hospitalName: '',
    admissionDate: '',
    dischargeDate: '',
    diagnosis: '',
  };

  errors: any = {};
  touched: any = {};

  constructor(
    private api: ApiService,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.api.getProfile().subscribe({
      next: (p) => {
        this.bankDetailsFilled = !!(p.accountNumber && p.ifscCode);
      }
    });

    this.api.getMyPolicies().subscribe({
      next: (d) => {
        this.policies = d.filter((p) => p.policyStatus === 'ACTIVE');
        if (this.policies.length)
          this.form.policyId = this.policies[0].policyId;
        this.cdr.detectChanges();
      },
    });
  }

  // Converts a Date to "yyyy-MM-dd" in LOCAL time (avoids UTC shift)
  toLocalDateString(date: Date): string {
    const yyyy = date.getFullYear();
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  }

  getSelectedPolicy(): PolicyResponse | undefined {
    return this.policies.find(p => p.policyId === +this.form.policyId);
  }

  getPolicyStartDateString(): string {
    const policy = this.getSelectedPolicy();
    if (!policy?.startDate) return '';
    return this.toLocalDateString(new Date(policy.startDate));
  }

  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }

  onFileSelect(event: any): void {
    this.selectedFiles = Array.from(event.target.files);
    if (this.selectedFiles.length > 0) delete this.errors.documents;
  }

  onBlur(field: string): void {
    this.touched[field] = true;
    this.validateField(field);
  }

  validateField(field: string): void {
    // Today at midnight local time — for admission date validation only
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    switch (field) {

      case 'policyId':
        if (!this.form.policyId)
          this.errors.policyId = 'Please select a policy';
        else
          delete this.errors.policyId;
        break;

      case 'hospitalName':
        if (!this.form.hospitalName?.trim())
          this.errors.hospitalName = 'Hospital name is required';
        else if (this.form.hospitalName.trim().length < 3)
          this.errors.hospitalName = 'Enter a valid hospital name';
        else
          delete this.errors.hospitalName;
        break;

      case 'claimAmount':
        if (!this.form.claimAmount || this.form.claimAmount <= 0) {
          this.errors.claimAmount = 'Valid claim amount is required';
        } else {
          const policy = this.getSelectedPolicy();
          const remaining = policy?.remainingCoverage || policy?.coverageAmount || 0;
          if (remaining > 0 && this.form.claimAmount > remaining) {
            this.errors.claimAmount =
              'Amount exceeds your remaining coverage of ' +
              this.formatCurrency(remaining);
          } else {
            delete this.errors.claimAmount;
          }
        }
        break;

      case 'admissionDate': {
        if (!this.form.admissionDate) {
          this.errors.admissionDate = 'Admission date is required';
        } else {
          // Parse directly to avoid UTC-to-local shift
          const [ay, am, ad] = this.form.admissionDate.split('-').map(Number);
          const admission = new Date(ay, am - 1, ad);

          // Admission must NOT be in the future
          if (admission > today) {
            this.errors.admissionDate = 'Admission date cannot be a future date';
          } else {
            const policy = this.getSelectedPolicy();
            if (policy?.startDate) {
              const policyStart = new Date(policy.startDate);
              policyStart.setHours(0, 0, 0, 0);
              if (admission < policyStart) {
                this.errors.admissionDate =
                  'Admission date cannot be before policy start (' +
                  policyStart.toLocaleDateString('en-IN', {
                    day: '2-digit', month: 'short', year: 'numeric',
                  }) + ')';
              } else {
                delete this.errors.admissionDate;
              }
            } else {
              delete this.errors.admissionDate;
            }
          }
        }
        // Re-validate discharge date whenever admission changes
        if (!this.errors.admissionDate && this.form.dischargeDate) {
          this.validateField('dischargeDate');
        }
        break;
      }

      case 'dischargeDate': {
        if (!this.form.dischargeDate) {
          this.errors.dischargeDate = 'Discharge date is required';
        } else {
          const [dy, dm, dd] = this.form.dischargeDate.split('-').map(Number);
          const discharge = new Date(dy, dm - 1, dd);

          // FIX 6: Discharge date CAN be today or in the future
          // (patient may still be admitted — only block before admission date)
          if (this.form.admissionDate) {
            const [ay, am, ad] = this.form.admissionDate.split('-').map(Number);
            const admission = new Date(ay, am - 1, ad);

            if (discharge < admission) {
              this.errors.dischargeDate =
                'Discharge date cannot be before admission date';
            } else {
              delete this.errors.dischargeDate;
            }
          } else {
            // No admission date set yet — just accept it
            delete this.errors.dischargeDate;
          }
        }
        break;
      }

      case 'diagnosis':
        if (!this.form.diagnosis?.trim())
          this.errors.diagnosis = 'Diagnosis is required';
        else if (this.form.diagnosis.trim().length < 3)
          this.errors.diagnosis = 'Please provide a more detailed diagnosis';
        else
          delete this.errors.diagnosis;
        break;
    }
  }

  submit(): void {
    this.errors = {};
    this.touched = {
      policyId: true,
      hospitalName: true,
      claimAmount: true,
      admissionDate: true,
      dischargeDate: true,
      diagnosis: true,
      documents: true,
    };

    ['policyId', 'hospitalName', 'claimAmount', 'admissionDate',
      'dischargeDate', 'diagnosis'].forEach(f => this.validateField(f));

    if (this.selectedFiles.length === 0) {
      this.errors.documents =
        'At least one supporting document is required (bill, prescription, or report)';
    }

    if (Object.keys(this.errors).length > 0) {
      this.toast.error('Please fix the errors before submitting');
      setTimeout(() => {
        const firstError = document.querySelector('.form-control.error, .error-message');
        if (firstError)
          (firstError as HTMLElement).scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 100);
      return;
    }

    this.filing = true;
    this.cdr.detectChanges();

    const formData = new FormData();
    formData.append(
      'claim',
      new Blob([JSON.stringify(this.form)], { type: 'application/json' }),
    );

    for (const file of this.selectedFiles) {
      formData.append('documents', file);
    }

    this.api.fileClaim(formData).subscribe({
      next: () => {
        this.toast.success('Claim filed successfully!');
        this.router.navigate(['/customer/claims']);
      },
      error: (err) => {
        this.filing = false;
        this.toast.error(err.error?.message || 'Failed to file claim');
        this.cdr.detectChanges();
      },
    });
  }
}
