import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../../core/services/api.service';
import { ToastService } from '../../../core/services/toast.service';

interface AgentRequest {
  id: number;
  planTypeInterest: string;
  preferredTime: string;
  message: string;
  status: string;
  underwriterName: string;
  createdAt: string;
  acceptedAt: string;
  completedAt: string;
  resultingPolicyNumber: string;
}

// Extended interface to hold pre-calculated UI properties
interface AgentRequestView extends AgentRequest {
  formattedPlanType: string;
  statusColor: string;
  statusIcon: string;
}

@Component({
  selector: 'app-request-agent',
  standalone: true,
  imports: [FormsModule, RouterLink, DatePipe], // CommonModule removed
  templateUrl: './request-agent.html',
  styleUrl: './request-agent.css',
})
export class RequestAgentComponent implements OnInit {

  myRequests: AgentRequestView[] = [];
  loading = true;
  submitting = false;
  cancellingId: number | null = null; // Tracks which request is currently being cancelled
  showForm = false;

  form = {
    planTypeInterest: '',
    preferredTime: '',
    message: ''
  };

  planTypes = ['INDIVIDUAL', 'FAMILY', 'SENIOR_CITIZEN', 'NOT_SURE'];

  constructor(
    private api: ApiService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadMyRequests();
  }

  loadMyRequests(isRefresh = false): void {
    if (!isRefresh) {
      this.loading = true;
    }

    this.api.getMyAgentRequests().subscribe({
      next: (data: AgentRequest[]) => {
        // Map UI properties once when data arrives
        this.myRequests = data.map(req => ({
          ...req,
          formattedPlanType: this.formatPlanType(req.planTypeInterest),
          statusColor: this.getStatusColor(req.status),
          statusIcon: this.getStatusIcon(req.status)
        }));
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  submitRequest(): void {
    if (!this.form.planTypeInterest) {
      this.toast.error('Please select your plan type interest');
      return;
    }
    this.submitting = true;
    this.api.createAgentRequest(this.form).subscribe({
      next: () => {
        this.submitting = false;
        this.showForm = false;
        this.form = { planTypeInterest: '', preferredTime: '', message: '' };
        this.toast.success('Request submitted! An agent will contact you soon.');
        this.loadMyRequests(true); // Silent refresh
      },
      error: (err: any) => {
        this.submitting = false;
        this.toast.error(err?.error?.message || 'Failed to submit request');
      }
    });
  }

  // ADD this new property
  confirmingCancelId: number | null = null;

// REPLACE cancelRequest with these two methods
  confirmCancel(id: number): void {
    this.confirmingCancelId = id;
  }

  cancelRequest(id: number): void {
    this.cancellingId = id;
    this.confirmingCancelId = null;

    this.api.cancelAgentRequest(id).subscribe({
      next: () => {
        this.cancellingId = null;
        this.toast.success('Request cancelled');
        this.loadMyRequests(true);
      },
      error: () => {
        this.cancellingId = null;
        this.toast.error('Failed to cancel request');
      }
    });
  }

  private getStatusColor(status: string): string {
    const map: Record<string, string> = {
      PENDING:   '#f59e0b',
      ACCEPTED:  '#3b82f6',
      COMPLETED: '#10b981',
      CANCELLED: '#ef4444'
    };
    return map[status] || '#94a3b8';
  }

  private getStatusIcon(status: string): string {
    const map: Record<string, string> = {
      PENDING:   'hourglass_top',
      ACCEPTED:  'person_search',
      COMPLETED: 'check_circle',
      CANCELLED: 'cancel'
    };
    return map[status] || 'help';
  }

  formatPlanType(type: string): string {
    return type.replace(/_/g, ' ');
  }
}
