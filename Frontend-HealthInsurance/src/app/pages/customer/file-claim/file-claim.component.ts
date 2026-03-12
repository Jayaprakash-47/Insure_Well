import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({ selector: 'app-file-claim', standalone: true, imports: [FormsModule], templateUrl: './file-claim.component.html', styleUrl: './file-claim.component.css' })
export class FileClaimComponent implements OnInit {
    policies: PolicyResponse[] = [];
    filing = false;
    selectedFiles: File[] = [];
    form = { policyId: 0, claimType: 'CASHLESS', claimAmount: 0, hospitalName: '', admissionDate: '', dischargeDate: '', diagnosis: '' };
    errors: any = {};
    touched: any = {};

    constructor(private api: ApiService, private toast: ToastService, private router: Router, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getMyPolicies().subscribe({ next: (d) => { this.policies = d.filter(p => p.policyStatus === 'ACTIVE'); if (this.policies.length) this.form.policyId = this.policies[0].policyId; this.cdr.detectChanges(); } });
    }

    onFileSelect(event: any): void {
        this.selectedFiles = Array.from(event.target.files);
        if (this.selectedFiles.length > 0) {
            delete this.errors.documents;
        }
    }

    onBlur(field: string): void {
        this.touched[field] = true;
        this.validateField(field);
    }

    validateField(field: string): void {
        switch (field) {
            case 'policyId':
                if (!this.form.policyId) { this.errors.policyId = 'Please select a policy'; } else { delete this.errors.policyId; }
                break;
            case 'hospitalName':
                if (!this.form.hospitalName?.trim()) { this.errors.hospitalName = 'Hospital name is required'; } else { delete this.errors.hospitalName; }
                break;
            case 'claimAmount':
                if (!this.form.claimAmount || this.form.claimAmount <= 0) { this.errors.claimAmount = 'Valid claim amount is required'; } else { delete this.errors.claimAmount; }
                break;
            case 'admissionDate':
                if (!this.form.admissionDate) { this.errors.admissionDate = 'Admission date is required'; } else { delete this.errors.admissionDate; }
                break;
            case 'dischargeDate':
                if (!this.form.dischargeDate) { this.errors.dischargeDate = 'Discharge date is required'; } else { delete this.errors.dischargeDate; }
                break;
            case 'diagnosis':
                if (!this.form.diagnosis?.trim()) { this.errors.diagnosis = 'Diagnosis is required'; } else { delete this.errors.diagnosis; }
                break;
        }
    }

    submit(): void {
        this.errors = {};
        this.touched = { policyId: true, hospitalName: true, claimAmount: true, admissionDate: true, dischargeDate: true, diagnosis: true, documents: true };
        let isValid = true;

        if (!this.form.policyId) {
            this.errors.policyId = 'Please select a policy';
            isValid = false;
        }
        if (!this.form.hospitalName?.trim()) {
            this.errors.hospitalName = 'Hospital name is required';
            isValid = false;
        }
        if (!this.form.claimAmount || this.form.claimAmount <= 0) {
            this.errors.claimAmount = 'Valid claim amount is required';
            isValid = false;
        }
        if (!this.form.admissionDate) {
            this.errors.admissionDate = 'Admission date is required';
            isValid = false;
        }
        if (!this.form.dischargeDate) {
            this.errors.dischargeDate = 'Discharge date is required';
            isValid = false;
        }
        if (!this.form.diagnosis?.trim()) {
            this.errors.diagnosis = 'Diagnosis is required';
            isValid = false;
        }
        if (this.selectedFiles.length === 0) {
            this.errors.documents = 'At least one supporting document is required (bill, prescription, or report)';
            isValid = false;
        }

        if (!isValid) {
            this.toast.error('Please fill all required fields and upload at least one document');
            setTimeout(() => {
                const firstError = document.querySelector('.form-control.error, .file-upload-error');
                if (firstError) {
                    (firstError as HTMLElement).scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
            }, 100);
            return;
        }

        this.filing = true;
        this.cdr.detectChanges();

        const formData = new FormData();
        formData.append('claim', new Blob([JSON.stringify(this.form)], { type: 'application/json' }));

        for (const file of this.selectedFiles) {
            formData.append('documents', file);
        }

        this.api.fileClaim(formData).subscribe({
            next: () => { this.toast.success('Claim and documents filed successfully!'); this.router.navigate(['/customer/claims']); },
            error: (err) => { this.filing = false; this.toast.error(err.error?.message || 'Failed to file claim'); this.cdr.detectChanges(); }
        });
    }
}
