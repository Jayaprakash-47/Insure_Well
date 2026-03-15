import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { of, throwError } from 'rxjs';

describe('LoginComponent', () => {
  let fixture: ComponentFixture<LoginComponent>;
  let component: LoginComponent;
  let authSpy: jasmine.SpyObj<AuthService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['login', 'getDashboardRoute', 'isLoggedIn']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    authSpy.getDashboardRoute.and.returnValue('/customer/dashboard');

    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // TC-49
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // TC-50
  it('validateField: should set email error when email is empty', () => {
    component.email = '';
    component.validateField('email');
    expect(component.errors.email).toBe('Email cannot be empty');
  });

  // TC-51
  it('validateField: should set email error for invalid format', () => {
    component.email = 'notanemail';
    component.validateField('email');
    expect(component.errors.email).toBe('Invalid email format');
  });

  // TC-52
  it('validateField: should clear email error for valid email', () => {
    component.email = 'valid@example.com';
    component.validateField('email');
    expect(component.errors.email).toBeUndefined();
  });

  // TC-53
  it('validateField: should set password error when password is empty', () => {
    component.password = '';
    component.validateField('password');
    expect(component.errors.password).toBe('Password cannot be empty');
  });

  // TC-54
  it('onSubmit: should call toast.error if validation fails', () => {
    component.email = '';
    component.password = '';
    component.onSubmit();
    expect(toastSpy.error).toHaveBeenCalled();
    expect(authSpy.login).not.toHaveBeenCalled();
  });

  // TC-55
  it('onSubmit: should call auth.login with correct credentials', () => {
    authSpy.login.and.returnValue(of({} as any));
    component.email = 'user@test.com';
    component.password = 'Password@1';
    component.onSubmit();
    expect(authSpy.login).toHaveBeenCalledWith({
      email: 'user@test.com',
      password: 'Password@1',
    });
  });

  // TC-56
  it('onSubmit: should call toast.success on successful login', () => {
    authSpy.login.and.returnValue(of({} as any));
    component.email = 'user@test.com';
    component.password = 'Password@1';
    component.onSubmit();
    expect(toastSpy.success).toHaveBeenCalledWith('Login successful!');
  });

  // TC-57
  it('onSubmit: should call toast.error on login failure', () => {
    authSpy.login.and.returnValue(
      throwError(() => ({ error: { message: 'Invalid credentials' } })),
    );
    component.email = 'user@test.com';
    component.password = 'Password@1';
    component.onSubmit();
    expect(toastSpy.error).toHaveBeenCalledWith('Invalid credentials');
  });

  // TC-58
  it('onBlur: should mark field as touched and validate it', () => {
    component.email = '';
    component.onBlur('email');
    expect(component.touched['email']).toBeTrue();
    expect(component.errors.email).toBeDefined();
  });
});
