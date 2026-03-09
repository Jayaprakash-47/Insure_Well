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

    private loggedIn = new BehaviorSubject<boolean>(this.hasToken());
    isLoggedIn$ = this.loggedIn.asObservable();

    constructor(private http: HttpClient, private router: Router) { }

    login(request: LoginRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/login`, request).pipe(
            tap(res => {
                this.storeSession(res);
                this.loggedIn.next(true);
            })
        );
    }

    register(request: RegisterRequest): Observable<AuthResponse> {
        return this.http.post<AuthResponse>(`${this.apiUrl}/auth/register`, request).pipe(
            tap(res => {
                this.storeSession(res);
                this.loggedIn.next(true);
            })
        );
    }

    logout(): void {
        localStorage.removeItem(this.tokenKey);
        localStorage.removeItem(this.userKey);
        this.loggedIn.next(false);
        this.router.navigate(['/login']);
    }

    getToken(): string | null {
        return localStorage.getItem(this.tokenKey);
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

    isAgent(): boolean {
        return this.getRole() === 'ROLE_AGENT';
    }

    isClaimsOfficer(): boolean {
        return this.getRole() === 'ROLE_CLAIMS_OFFICER';
    }

    getDashboardRoute(): string {
        const role = this.getRole();
        if (role === 'ROLE_ADMIN') return '/admin/dashboard';
        if (role === 'ROLE_AGENT') return '/agent/dashboard';
        if (role === 'ROLE_CLAIMS_OFFICER') return '/claims-officer/dashboard';
        return '/customer/dashboard';
    }

    private storeSession(res: AuthResponse): void {
        localStorage.setItem(this.tokenKey, res.token);
        localStorage.setItem(this.userKey, JSON.stringify({
            userId: res.userId,
            firstName: res.firstName,
            email: res.email,
            role: res.role
        }));
    }

    private hasToken(): boolean {
        return !!localStorage.getItem(this.tokenKey);
    }
}
