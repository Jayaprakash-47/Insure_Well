import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';
import { CreateClaimsOfficerRequest } from '../../../core/models/models';

@Component({
    selector: 'app-manage-claims-officers',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './manage-claims-officers.component.html',
    styleUrl: './manage-claims-officers.component.css'
})
export class ManageClaimsOfficersComponent implements OnInit {
    officers: any[] = [];
    loading = true;
    showModal = false;
    submitting = false;

    form: CreateClaimsOfficerRequest = {
        firstName: '', lastName: '', email: '', password: '',
        phone: '', employeeId: '', department: '', approvalLimit: 500000
    };

    constructor(private api: ApiService, private toast: ToastService) { }

    ngOnInit(): void { this.loadOfficers(); }

    loadOfficers(): void {
        this.loading = true;
        this.api.getAllClaimsOfficers().subscribe({
            next: (data) => { this.officers = data; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    openCreate(): void {
        this.form = { firstName: '', lastName: '', email: '', password: '', phone: '', employeeId: '', department: 'Claims Processing', approvalLimit: 500000 };
        this.showModal = true;
    }

    create(): void {
        if (!this.form.email || !this.form.password || !this.form.firstName) {
            this.toast.error('Please fill all required fields');
            return;
        }
        this.submitting = true;
        this.api.createClaimsOfficer(this.form).subscribe({
            next: () => { this.toast.success('Claims Officer created'); this.showModal = false; this.loadOfficers(); this.submitting = false; },
            error: (err) => { this.toast.error(err.error?.message || 'Failed'); this.submitting = false; }
        });
    }

    toggleStatus(officer: any): void {
        const action = officer.isActive ? this.api.deactivateUser(officer.userId) : this.api.activateUser(officer.userId);
        action.subscribe({
            next: () => {
                this.toast.success(officer.isActive ? 'Claims Officer deactivated' : 'Claims Officer activated');
                this.loadOfficers();
            },
            error: () => { this.toast.error('Action failed'); }
        });
    }
}
