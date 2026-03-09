import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from '../../../core/services/api.service';
import { AuditLogResponse } from '../../../core/models/models';

@Component({
    selector: 'app-audit-logs',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './audit-logs.component.html',
    styleUrl: './audit-logs.component.css'
})
export class AuditLogsComponent implements OnInit {
    logs: AuditLogResponse[] = [];
    loading = true;

    constructor(private api: ApiService) { }

    ngOnInit(): void {
        this.api.getAuditLogs().subscribe({
            next: (data) => { this.logs = data; this.loading = false; },
            error: () => { this.loading = false; }
        });
    }

    getActionClass(action: string): string {
        if (action.includes('CREATED') || action.includes('CREATION')) return 'action-create';
        if (action.includes('STATUS_CHANGE')) return 'action-update';
        if (action.includes('SETTLED') || action.includes('SETTLEMENT')) return 'action-settle';
        return 'action-default';
    }

    getActionIcon(action: string): string {
        if (action.includes('CREATED') || action.includes('CREATION')) return '🆕';
        if (action.includes('STATUS_CHANGE')) return '🔄';
        if (action.includes('SETTLED') || action.includes('SETTLEMENT')) return '💰';
        return '📝';
    }
}
