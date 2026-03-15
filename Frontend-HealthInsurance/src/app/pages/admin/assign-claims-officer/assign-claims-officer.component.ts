import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../../core/services/api.service';

@Component({
  selector: 'app-assign-claims-officer',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './assign-claims-officer.component.html',
  styleUrl: './assign-claims-officer.component.css',
})
export class AssignClaimsOfficerComponent implements OnInit {
  claims: any[] = [];
  officers: any[] = [];
  selectedClaim: any = null;
  selectedOfficerId: number | null = null;
  loadingClaims = true;
  loadingOfficers = false;
  assigning = false;
  success = false;
  error = '';

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getSubmittedClaims().subscribe({
      next: (d) => {
        this.claims = d;
        this.loadingClaims = false;
      },
      error: () => {
        this.loadingClaims = false;
      },
    });
    this.loadingOfficers = true;
    this.api.getAllClaimsOfficers().subscribe({
      next: (d) => {
        this.officers = d.filter((o: any) => o.isActive);
        this.loadingOfficers = false;
      },
      error: () => {
        this.loadingOfficers = false;
      },
    });
  }

  selectClaim(c: any) {
    this.selectedClaim = c;
    this.selectedOfficerId = null;
    this.success = false;
    this.error = '';
  }

  assignOfficer() {
    if (!this.selectedClaim || !this.selectedOfficerId) return;
    this.assigning = true;
    this.error = '';
    this.success = false;
    this.api
      .assignClaimsOfficer(this.selectedClaim.claimId, { claimsOfficerId: this.selectedOfficerId })
      .subscribe({
        next: () => {
          this.assigning = false;
          this.success = true;
          this.claims = this.claims.filter((c) => c.claimId !== this.selectedClaim.claimId);
          setTimeout(() => {
            this.selectedClaim = null;
            this.selectedOfficerId = null;
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
