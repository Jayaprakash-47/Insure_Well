import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-assign-underwriter',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './assign-underwriter.component.html',
  styleUrl: './assign-underwriter.component.css',
})
export class AssignUnderwriterComponent implements OnInit {
  pendingApps: any[] = [];
  underwriters: any[] = [];
  selectedApp: any = null;
  selectedUwId: number | null = null;
  loadingApps = true;
  loadingUw = false;
  assigning = false;
  success = false;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getPendingPolicyApplications().subscribe({
      next: (d) => {
        this.pendingApps = d;
        this.loadingApps = false;
      },
      error: () => {
        this.loadingApps = false;
      },
    });
    this.loadingUw = true;
    this.api.getAllUnderwriters().subscribe({
      next: (d) => {
        this.underwriters = d.filter((u: any) => u.isActive);
        this.loadingUw = false;
      },
      error: () => {
        this.loadingUw = false;
      },
    });
  }

  selectApp(app: any) {
    this.selectedApp = app;
    this.selectedUwId = null;
    this.success = false;
    this.error = '';
  }

  assignUnderwriter() {
    if (!this.selectedApp || !this.selectedUwId) return;
    this.assigning = true;
    this.error = '';
    this.success = false;
    this.api
      .assignUnderwriter(this.selectedApp.policyId, { underwriterId: this.selectedUwId })
      .subscribe({
        next: () => {
          this.assigning = false;
          this.success = true;
          this.pendingApps = this.pendingApps.filter(
            (a) => a.policyId !== this.selectedApp.policyId,
          );
          setTimeout(() => {
            this.selectedApp = null;
            this.selectedUwId = null;
            this.success = false;
          }, 2000);
        },
        error: (err) => {
          this.assigning = false;
          this.error = err?.error?.message || 'Assignment failed.';
        },
      });
  }
}
