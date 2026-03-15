import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ForgotPasswordComponent } from './forgot-password';
import { AuthService } from '../../../../core/services/auth.service';
import { of, throwError } from 'rxjs';

describe('ForgotPasswordComponent', () => {
  let fixture: ComponentFixture<ForgotPasswordComponent>;
  let component: ForgotPasswordComponent;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['sendForgotPasswordOtp', 'resetPassword']);

    await TestBed.configureTestingModule({
      imports: [ForgotPasswordComponent, RouterTestingModule],
      providers: [{ provide: AuthService, useValue: authSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ForgotPasswordComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // TC-73
  it('should create and start on step 1', () => {
    expect(component).toBeTruthy();
    expect(component.step).toBe(1);
  });

  // TC-74
  it('sendOtp: should show error when email is empty', () => {
    component.email = '';
    component.sendOtp();
    expect(component.message).toBe('Please enter your email');
    expect(component.isError).toBeTrue();
    expect(authSpy.sendForgotPasswordOtp).not.toHaveBeenCalled();
  });

  // TC-75
  it('sendOtp: should advance to step 2 on success', () => {
    authSpy.sendForgotPasswordOtp.and.returnValue(of({}));
    component.email = 'user@test.com';
    component.sendOtp();
    expect(component.step).toBe(2);
  });

  // TC-76
  it('sendOtp: should show error on failure', () => {
    authSpy.sendForgotPasswordOtp.and.returnValue(
      throwError(() => ({ error: { error: 'Email not found' } })),
    );
    component.email = 'user@test.com';
    component.sendOtp();
    expect(component.isError).toBeTrue();
    expect(component.message).toBe('Email not found');
  });

  // TC-77
  it('verifyOtp: should show error when OTP length != 6', () => {
    component.otp = '123';
    component.verifyOtp();
    expect(component.message).toBe('Enter a valid 6-digit OTP');
    expect(component.step).toBe(1);
  });

  // TC-78
  it('verifyOtp: should advance to step 3 for valid 6-digit OTP', () => {
    component.step = 2;
    component.otp = '654321';
    component.verifyOtp();
    expect(component.step).toBe(3);
  });

  // TC-79
  it('resetPassword: should show error when passwords do not match', () => {
    component.newPassword = 'Pass@1234';
    component.confirmPassword = 'Different@1';
    component.resetPassword();
    expect(component.message).toBe('Passwords do not match');
  });

  // TC-80
  it('resetPassword: should show error for short password', () => {
    component.newPassword = 'Short1!';
    component.confirmPassword = 'Short1!';
    component.resetPassword();
    expect(component.message).toBe('Password must be at least 8 characters');
  });

  // TC-81
  it('resetPassword: should call auth.resetPassword on valid input', () => {
    authSpy.resetPassword.and.returnValue(of({}));
    component.email = 'user@test.com';
    component.otp = '123456';
    component.newPassword = 'NewPass@1';
    component.confirmPassword = 'NewPass@1';
    component.resetPassword();
    expect(authSpy.resetPassword).toHaveBeenCalledWith('user@test.com', '123456', 'NewPass@1');
  });

  // TC-82
  it('resetPassword: should set success message on success', () => {
    authSpy.resetPassword.and.returnValue(of({}));
    component.email = 'user@test.com';
    component.otp = '123456';
    component.newPassword = 'NewPass@1';
    component.confirmPassword = 'NewPass@1';
    component.resetPassword();
    expect(component.message).toContain('Password reset successful');
    expect(component.isError).toBeFalse();
  });
});
