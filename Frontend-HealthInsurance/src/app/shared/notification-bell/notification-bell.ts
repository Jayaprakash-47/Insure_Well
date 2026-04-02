import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificationService, AppNotification } from '../../core/services/notification.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-notification-bell',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-bell.html',
  styleUrl: './notification-bell.css',
})
export class NotificationBellComponent implements OnInit {
  isOpen = false;
  notifications$: Observable<AppNotification[]>;
  unreadCount$: Observable<number>;

  constructor(private notifService: NotificationService) {
    this.notifications$ = this.notifService.notifications;
    this.unreadCount$ = this.notifService.unreadCount$;
  }

  ngOnInit(): void {}

  togglePanel(e: Event): void {
    e.stopPropagation();
    this.isOpen = !this.isOpen;
  }

  markAllRead(): void {
    this.notifService.markAllRead().subscribe();
  }

  getIcon(type: string): string {
    const icons: Record<string, string> = {
      POLICY_APPROVED: '✅',
      POLICY_REJECTED: '❌',
      POLICY_EXPIRED: '⏰',
      POLICY_RENEWED: '🔄',
      RENEWAL_DUE: '📅',
      CLAIM_SUBMITTED: '📋',
      CLAIM_APPROVED: '💰',
      CLAIM_REJECTED: '🚫',
      QUOTE_RECEIVED: '📨',
      GENERAL: '🔔',
    };
    return icons[type] || '🔔';
  }

  // Close panel when clicking outside
  @HostListener('document:click')
  onClickOutside(): void {
    this.isOpen = false;
  }
}
