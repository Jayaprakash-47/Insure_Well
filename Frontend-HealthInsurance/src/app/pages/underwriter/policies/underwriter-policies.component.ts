import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-underwriter-policies',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './underwriter-policies.component.html',
  styleUrl: './underwriter-policies.component.css',
})
export class UnderwriterPoliciesComponent implements OnInit {
  policies: PolicyResponse[] = [];
  loading = true;

  constructor(private api: ApiService) {}

  ngOnInit() {
    this.api.getUnderwriterMyPolicies().subscribe({
      next: (d) => {
        this.policies = d;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  getStatusClass(status: string): string {
    switch ((status || '').toUpperCase()) {
      case 'ACTIVE':
        return 'status-badge status-active';
      case 'ASSIGNED':
        return 'status-badge status-assigned';
      case 'QUOTE_SENT':
        return 'status-badge status-quote_sent';
      case 'PENDING':
        return 'status-badge status-pending';
      default:
        return 'status-badge status-other';
    }
  }
}
