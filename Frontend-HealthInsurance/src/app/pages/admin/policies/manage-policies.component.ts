import { Component, OnInit, ChangeDetectorRef } from '@angular/core';

import { ApiService } from '../../../core/services/api.service';
import { PolicyResponse } from '../../../core/models/models';

@Component({
  selector: 'app-manage-policies',
  standalone: true,
  imports: [],
  templateUrl: './manage-policies.component.html',
  styleUrl: './manage-policies.component.css',
})
export class ManagePoliciesComponent implements OnInit {
  policies: PolicyResponse[] = [];
  loading = true;
  constructor(
    private api: ApiService,
    private cdr: ChangeDetectorRef,
  ) {}
  ngOnInit(): void {
    this.api.getAllPolicies().subscribe({
      next: (d) => {
        this.policies = d;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }
  formatCurrency(n: number): string {
    return '₹' + (n || 0).toLocaleString('en-IN');
  }
}
