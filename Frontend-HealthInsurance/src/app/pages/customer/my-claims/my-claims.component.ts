import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ClaimResponse } from '../../../core/models/models';
@Component({ selector: 'app-my-claims', standalone: true, imports: [CommonModule, RouterLink], templateUrl: './my-claims.component.html', styleUrl: './my-claims.component.css' })
export class MyClaimsComponent implements OnInit {
    claims: ClaimResponse[] = []; loading = true;
    constructor(private api: ApiService, private cdr: ChangeDetectorRef) { }
    ngOnInit(): void { this.api.getMyClaims().subscribe({ next: (d) => { this.claims = d; this.loading = false; this.cdr.detectChanges(); }, error: () => { this.loading = false; this.cdr.detectChanges(); } }); }
    formatCurrency(n: number): string { return '₹' + (n || 0).toLocaleString('en-IN'); }
}
