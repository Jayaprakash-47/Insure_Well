import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { map, tap } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

export interface AppNotification {
  id: number;
  message: string;
  type: string;
  read: boolean; // ← matches what Java sends (Jackson strips "is" prefix)
  createdAt: string;
  recipientEmail: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private apiUrl = environment.apiUrl;
  private notifications$ = new BehaviorSubject<AppNotification[]>([]);
  private eventSource: EventSource | null = null;

  notifications = this.notifications$.asObservable();

  // ← use n.read everywhere (not n.isRead)
  unreadCount$ = this.notifications.pipe(map((list) => list.filter((n) => !n.read).length));

  constructor(
    private http: HttpClient,
    private auth: AuthService,
  ) {}

  connect(): void {
    if (this.eventSource) return;

    const token = this.auth.getToken();
    if (!token) return;

    this.loadUnread().subscribe();

    this.eventSource = new EventSource(`${this.apiUrl}/notifications/subscribe?token=${token}`);

    this.eventSource.addEventListener('notification', (e: any) => {
      const notif: AppNotification = JSON.parse(e.data);
      this.notifications$.next([notif, ...this.notifications$.getValue()]);
    });

    this.eventSource.addEventListener('ping', () => {
      // Connection confirmed — no action needed
    });

    this.eventSource.onerror = () => {
      this.disconnect();
      setTimeout(() => this.connect(), 5000);
    };
  }

  disconnect(): void {
    this.eventSource?.close();
    this.eventSource = null;
  }

  loadUnread(): Observable<AppNotification[]> {
    return this.http
      .get<AppNotification[]>(`${this.apiUrl}/notifications`)
      .pipe(tap((list) => this.notifications$.next(list)));
  }

  markAllRead(): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/notifications/mark-read`, {}).pipe(
      tap(() => {
        // ← use n.read (not n.isRead)
        const updated = this.notifications$.getValue().map((n) => ({ ...n, read: true }));
        this.notifications$.next(updated);
      }),
    );
  }

  getUnreadCount(): Observable<number> {
    return this.http
      .get<{ count: number }>(`${this.apiUrl}/notifications/count`)
      .pipe(map((res) => res.count));
  }
}
