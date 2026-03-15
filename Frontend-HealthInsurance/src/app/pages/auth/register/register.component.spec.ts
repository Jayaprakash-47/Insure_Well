import { TestBed, ComponentFixture } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { of, throwError } from 'rxjs';

describe('RegisterComponent', () => {
  let fixture: ComponentFixture<RegisterComponent>;
  let component: RegisterComponent;
  let authSpy: jasmine.SpyObj<AuthService>;
  let toastSpy: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    authSpy = jasmine.createSpyObj('AuthService', ['register']);
    toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);

    await TestBed.configureTestingModule({
      imports: [RegisterComponent, RouterTestingModule, HttpClientTestingModule],
      providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: ToastService, useValue: toastSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // TC-59
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // TC-60
  it('validateField: firstName — should set error when empty', () => {
    component.form.firstName = '';
    component.validateField('firstName');
    expect(component.errors.firstName).toBe('First name cannot be empty');
  });

  // TC-61
  it('validateField: firstName — should clear error for valid name', () => {
    component.form.firstName = 'Alice';
    component.validateField('firstName');
    expect(component.errors.firstName).toBeUndefined();
  });

  // TC-62
  it('validateField: email — should set error for invalid email', () => {
    component.form.email = 'bademail';
    component.validateField('email');
    expect(component.errors.email).toBe('Invalid email format');
  });

  // TC-63
  it('validateField: password — should set error when too short', () => {
    component.form.password = 'abc';
    component.validateField('password');
    expect(component.errors.password).toBe('Password must be at least 8 characters');
  });

  // TC-64
  it('validateField: password — should set error when no uppercase', () => {
    component.form.password = 'abcdefg1!';
    component.validateField('password');
    expect(component.errors.password).toBe('Password must contain at least one uppercase letter');
  });

  // TC-65
  it('validateField: password — should set error when no number', () => {
    component.form.password = 'Abcdefg!';
    component.validateField('password');
    expect(component.errors.password).toBe('Password must contain at least one number');
  });

  // TC-66
  it('validateField: password — should set error when no special char', () => {
    component.form.password = 'Abcdefg1';
    component.validateField('password');
    expect(component.errors.password).toBe('Password must contain at least one special character');
  });

  // TC-67
  it('validateField: password — should clear error for strong password', () => {
    component.form.password = 'StrongPass@1';
    component.validateField('password');
    expect(component.errors.password).toBeUndefined();
  });

  // TC-68
  it('validateField: phone — should set error for non-10-digit phone', () => {
    component.form.phone = '12345';
    component.validateField('phone');
    expect(component.errors.phone).toBe('Phone number must be 10 digits');
  });

  // TC-69
  it('validateField: phone — should clear error for valid phone', () => {
    component.form.phone = '9876543210';
    component.validateField('phone');
    expect(component.errors.phone).toBeUndefined();
  });

  // TC-70
  it('onSubmit: should call toast.error when form is invalid', () => {
    component.form.firstName = '';
    component.onSubmit();
    expect(toastSpy.error).toHaveBeenCalledWith('Please fill in all required fields correctly');
    expect(authSpy.register).not.toHaveBeenCalled();
  });

  // TC-71
  it('onSubmit: should call auth.register when form is valid', () => {
    authSpy.register.and.returnValue(of({} as any));
    component.form = {
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@test.com',
      password: 'StrongPass@1',
      phone: '9876543210',
      dateOfBirth: '1995-01-01',
      gender: 'FEMALE',
      address: '123 St',
      city: 'Chennai',
      state: 'TN',
      pincode: '600001',
    };
    component.onSubmit();
    expect(authSpy.register).toHaveBeenCalled();
  });

  // TC-72
  it('onSubmit: should call toast.error on register failure', () => {
    authSpy.register.and.returnValue(
      throwError(() => ({ error: { message: 'Email already in use' } })),
    );
    component.form = {
      firstName: 'Alice',
      lastName: 'Smith',
      email: 'alice@test.com',
      password: 'StrongPass@1',
      phone: '9876543210',
      dateOfBirth: '1995-01-01',
      gender: 'FEMALE',
      address: '123 St',
      city: 'Chennai',
      state: 'TN',
      pincode: '600001',
    };
    component.onSubmit();
    expect(toastSpy.error).toHaveBeenCalledWith('Email already in use');
  });
});
