import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/models';

const mockAuthResponse: AuthResponse = {
  token: 'header.eyJyb2xlIjoiUk9MRV9DVVNUT01FUiIsInN1YiI6InRlc3RAZXhhbXBsZS5jb20ifQ.sig',
  type: 'Bearer',
  userId: 1,
  firstName: 'Test',
  email: 'test@example.com',
  role: 'ROLE_CUSTOMER',
  message: 'Success',
  refreshToken: 'refresh_token_abc',
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  // TC-11
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // TC-12
  it('login() should POST and store session', () => {
    service.login({ email: 'test@example.com', password: 'Pass@123' }).subscribe((res) => {
      expect(res.token).toBe(mockAuthResponse.token);
    });
    const req = httpMock.expectOne((r) => r.url.includes('/auth/login'));
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);
    expect(localStorage.getItem('hs_token')).toBe(mockAuthResponse.token);
  });

  // TC-13
  it('register() should POST and store session', () => {
    service.register({} as any).subscribe((res) => {
      expect(res.userId).toBe(1);
    });
    const req = httpMock.expectOne((r) => r.url.includes('/auth/register'));
    req.flush(mockAuthResponse);
    expect(localStorage.getItem('hs_token')).toBe(mockAuthResponse.token);
  });

  // TC-14
  it('logout() should clear localStorage', () => {
    localStorage.setItem('hs_token', 'abc');
    localStorage.setItem('hs_user', '{}');
    service.logout();
    expect(localStorage.getItem('hs_token')).toBeNull();
    expect(localStorage.getItem('hs_user')).toBeNull();
  });

  // TC-15
  it('getToken() should return stored token', () => {
    localStorage.setItem('hs_token', 'mytoken');
    expect(service.getToken()).toBe('mytoken');
  });

  // TC-16
  it('isLoggedIn() returns false when no token', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  // TC-17
  it('isLoggedIn() returns true when token present', () => {
    localStorage.setItem('hs_token', 'some_token');
    expect(service.isLoggedIn()).toBeTrue();
  });

  // TC-18
  it('getUserName() returns firstName from stored user', () => {
    localStorage.setItem('hs_user', JSON.stringify({ firstName: 'Alice' }));
    expect(service.getUserName()).toBe('Alice');
  });

  // TC-19
  it('getUserName() returns empty string when no user stored', () => {
    expect(service.getUserName()).toBe('');
  });

  // TC-20
  it('getUserEmail() returns email from stored user', () => {
    localStorage.setItem('hs_user', JSON.stringify({ email: 'alice@test.com' }));
    expect(service.getUserEmail()).toBe('alice@test.com');
  });

  // TC-21
  it('getDashboardRoute() returns admin route for ROLE_ADMIN', () => {
    // Use a valid JWT with role ROLE_ADMIN (base64 encoded)
    const payload = btoa(JSON.stringify({ role: 'ROLE_ADMIN', sub: 'admin@test.com' }));
    localStorage.setItem('hs_token', `header.${payload}.sig`);
    expect(service.getDashboardRoute()).toBe('/admin/dashboard');
  });

  // TC-22
  it('getDashboardRoute() returns customer route for ROLE_CUSTOMER', () => {
    const payload = btoa(JSON.stringify({ role: 'ROLE_CUSTOMER', sub: 'cust@test.com' }));
    localStorage.setItem('hs_token', `header.${payload}.sig`);
    expect(service.getDashboardRoute()).toBe('/customer/dashboard');
  });

  // TC-23
  it('getDashboardRoute() returns underwriter route for ROLE_UNDERWRITER', () => {
    const payload = btoa(JSON.stringify({ role: 'ROLE_UNDERWRITER' }));
    localStorage.setItem('hs_token', `header.${payload}.sig`);
    expect(service.getDashboardRoute()).toBe('/underwriter/dashboard');
  });

  // TC-24
  it('getDashboardRoute() returns claims-officer route for ROLE_CLAIMS_OFFICER', () => {
    const payload = btoa(JSON.stringify({ role: 'ROLE_CLAIMS_OFFICER' }));
    localStorage.setItem('hs_token', `header.${payload}.sig`);
    expect(service.getDashboardRoute()).toBe('/claims-officer/dashboard');
  });

  // TC-25
  it('isAdmin() returns true for admin token', () => {
    const payload = btoa(JSON.stringify({ role: 'ROLE_ADMIN' }));
    localStorage.setItem('hs_token', `h.${payload}.s`);
    expect(service.isAdmin()).toBeTrue();
  });

  // TC-26
  it('isCustomer() returns true for customer token', () => {
    const payload = btoa(JSON.stringify({ role: 'ROLE_CUSTOMER' }));
    localStorage.setItem('hs_token', `h.${payload}.s`);
    expect(service.isCustomer()).toBeTrue();
  });

  // TC-27
  it('sendForgotPasswordOtp() should POST to forgot-password endpoint', () => {
    service.sendForgotPasswordOtp('user@test.com').subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/auth/forgot-password'));
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'OTP sent' });
  });

  // TC-28
  it('resetPassword() should POST to reset-password endpoint', () => {
    service.resetPassword('user@test.com', '123456', 'NewPass@1').subscribe();
    const req = httpMock.expectOne((r) => r.url.includes('/auth/reset-password'));
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Password reset' });
  });
});
