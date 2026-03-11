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

    constructor(private api: ApiService, private toast: ToastService, private router: Router, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.api.getMyPolicies().subscribe({ next: (d) => { this.policies = d.filter(p => p.policyStatus === 'ACTIVE'); if (this.policies.length) this.form.policyId = this.policies[0].policyId; this.cdr.detectChanges(); } });
    }

    onFileSelect(event: any): void {
        this.selectedFiles = Array.from(event.target.files);
    }

    submit(): void {
        if (!this.form.policyId || !this.form.hospitalName || !this.form.claimAmount) { this.toast.error('Fill all required fields'); return; }
        this.filing = true; this.cdr.detectChanges();

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
