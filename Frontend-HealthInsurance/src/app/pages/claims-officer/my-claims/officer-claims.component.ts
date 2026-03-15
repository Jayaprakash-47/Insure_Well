import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ClaimResponse } from '../../../core/models/models';

@Component({
  selector: 'app-officer-claims',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './officer-claims.component.html',
  styleUrl: './officer-claims.component.css',
})
export class OfficerClaimsComponent implements OnInit {
  claims: ClaimResponse[] = [];
  loading = true;
  filter = 'ALL';

  constructor(private api: ApiService) {}

  ngOnInit(): void {
    this.loadClaims();
  }

  loadClaims(): void {
    this.loading = true;
    this.api.getOfficerAssignedClaims().subscribe({
      next: (data) => {
        this.claims = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  get filteredClaims(): ClaimResponse[] {
    if (this.filter === 'ALL') return this.claims;
    return this.claims.filter((c) => c.claimStatus === this.filter);
  }

  setFilter(f: string): void {
    this.filter = f;
  }

  formatCurrency(amount: number): string {
    return '₹' + (amount || 0).toLocaleString('en-IN');
  }

  getStatusClass(status: string): string {
    const map: Record<string, string> = {
      SUBMITTED: 'badge-submitted',
      UNDER_REVIEW: 'badge-info',
      APPROVED: 'badge-approved',
      REJECTED: 'badge-rejected',
      ESCALATED: 'badge-info',
      DOCUMENT_PENDING: 'badge-pending',
      PARTIALLY_APPROVED: 'badge-info',
      SETTLED: 'badge-success',
    };
    return map[status] || 'badge-info';
  }
}
