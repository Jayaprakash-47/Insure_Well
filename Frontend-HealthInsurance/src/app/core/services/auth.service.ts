import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthResponse, LoginRequest, RegisterRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = environment.apiUrl;
  private tokenKey = 'hs_token';
  private userKey = 'hs_user';
  private refreshKey = 'hs_refresh'; // ← NEW

  private loggedIn = new BehaviorSubject<boolean>(this.hasToken());
  isLoggedIn$ = this.loggedIn.asObservable();

  constructor(
    private http: HttpClient,
    private router: Router,
  ) {}

  // ── existing methods (unchanged) ──

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request).pipe(
      tap((res) => {
        this.storeSession(res);
        this.loggedIn.next(true);
      }),
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, request).pipe(
      tap((res) => {
        this.storeSession(res);
        this.loggedIn.next(true);
      }),
    );
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    localStorage.removeItem(this.refreshKey); // ← NEW
    this.loggedIn.next(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getRefreshToken(): string | null {
    // ← NEW
    return localStorage.getItem(this.refreshKey);
  }

  getUser(): any {
    const user = localStorage.getItem(this.userKey);
    return user ? JSON.parse(user) : null;
  }

  getRole(): string {
    const token = this.getToken();
    if (!token) return '';
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role || '';
    } catch {
      return '';
    }
  }

  getUserName(): string {
    const user = this.getUser();
    return user?.firstName || '';
  }

  getUserEmail(): string {
    // ← NEW (used by notification service)
    const user = this.getUser();
    return user?.email || '';
  }

  getUserRole(): string {
    const user = this.getUser();
    return user?.role || this.getRole().replace('ROLE_', '');
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  isAdmin(): boolean {
    return this.getRole() === 'ROLE_ADMIN';
  }

  isCustomer(): boolean {
    return this.getRole() === 'ROLE_CUSTOMER';
  }

  isUnderwriter(): boolean {
    return this.getRole() === 'ROLE_UNDERWRITER';
  }

  isClaimsOfficer(): boolean {
    return this.getRole() === 'ROLE_CLAIMS_OFFICER';
  }

  getDashboardRoute(): string {
    const role = this.getRole();
    if (role === 'ROLE_ADMIN') return '/admin/dashboard';
    if (role === 'ROLE_UNDERWRITER') return '/underwriter/dashboard';
    if (role === 'ROLE_CLAIMS_OFFICER') return '/claims-officer/dashboard';
    return '/customer/dashboard';
  }

  // ── NEW: Refresh token ──
  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    return this.http.post<any>(`${this.apiUrl}/auth/refresh`, { refreshToken }).pipe(
      tap((res) => {
        localStorage.setItem(this.tokenKey, res.accessToken);
        localStorage.setItem(this.refreshKey, res.refreshToken);
      }),
    );
  }

  // ── NEW: Forgot password ──
  sendForgotPasswordOtp(email: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/auth/forgot-password`,
      { email },
      { responseType: 'json' },
    );
  }

  resetPassword(email: string, otp: string, newPassword: string): Observable<any> {
    return this.http.post(
      `${this.apiUrl}/auth/reset-password`,
      { email, otp, newPassword },
      { responseType: 'json' },
    );
  }

  // ── NEW: PDF download ──
  downloadPolicyCertificate(policyId: number): void {
    const token = this.getToken();
    const url = `${this.apiUrl}/pdf/policy/${policyId}/certificate`;
    fetch(url, { headers: { Authorization: `Bearer ${token}` } })
      .then((res) => res.blob())
      .then((blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = `policy_${policyId}_certificate.pdf`;
        a.click();
        URL.revokeObjectURL(a.href);
      });
  }

  private storeSession(res: AuthResponse): void {
    localStorage.setItem(this.tokenKey, res.token);
    localStorage.setItem(this.refreshKey, res.refreshToken); // ← NEW
    localStorage.setItem(
      this.userKey,
      JSON.stringify({
        userId: res.userId,
        firstName: res.firstName,
        email: res.email,
        role: res.role,
      }),
    );
  }

  private hasToken(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }
}
