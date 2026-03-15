import { TestBed } from '@angular/core/testing';
import { AbstractControl, FormControl, FormGroup } from '@angular/forms';
import {
  noFutureDateValidator,
  minAgeValidator,
  strongPasswordValidator,
  passwordMatchValidator,
} from './validators';

describe('Custom Validators', () => {
  // TC-29
  it('noFutureDateValidator: should return null for past date', () => {
    const fn = noFutureDateValidator();
    const ctrl = new FormControl('2000-01-01');
    expect(fn(ctrl)).toBeNull();
  });

  // TC-30
  it('noFutureDateValidator: should return error for future date', () => {
    const fn = noFutureDateValidator();
    const ctrl = new FormControl('2099-01-01');
    expect(fn(ctrl)).toEqual({ futureDate: true });
  });

  // TC-31
  it('noFutureDateValidator: should return null when value is empty', () => {
    const fn = noFutureDateValidator();
    const ctrl = new FormControl('');
    expect(fn(ctrl)).toBeNull();
  });

  // TC-32
  it('minAgeValidator: should return null for age above minimum', () => {
    const fn = minAgeValidator(18);
    const date = new Date();
    date.setFullYear(date.getFullYear() - 25);
    const ctrl = new FormControl(date.toISOString().split('T')[0]);
    expect(fn(ctrl)).toBeNull();
  });

  // TC-33
  it('minAgeValidator: should return error for age below minimum', () => {
    const fn = minAgeValidator(18);
    const date = new Date();
    date.setFullYear(date.getFullYear() - 15);
    const ctrl = new FormControl(date.toISOString().split('T')[0]);
    expect(fn(ctrl)).toEqual({ underage: { required: 18 } });
  });

  // TC-34
  it('strongPasswordValidator: should return null for strong password', () => {
    const fn = strongPasswordValidator();
    const ctrl = new FormControl('StrongPass@1');
    expect(fn(ctrl)).toBeNull();
  });

  // TC-35
  it('strongPasswordValidator: should return error for weak password (no uppercase)', () => {
    const fn = strongPasswordValidator();
    const ctrl = new FormControl('weakpass1!');
    expect(fn(ctrl)).toEqual({ weakPassword: true });
  });

  // TC-36
  it('strongPasswordValidator: should return error for short password', () => {
    const fn = strongPasswordValidator();
    const ctrl = new FormControl('Ab1!');
    expect(fn(ctrl)).toEqual({ weakPassword: true });
  });

  // TC-37
  it('strongPasswordValidator: should return error for password without special char', () => {
    const fn = strongPasswordValidator();
    const ctrl = new FormControl('Password123');
    expect(fn(ctrl)).toEqual({ weakPassword: true });
  });

  // TC-38
  it('passwordMatchValidator: should return null when passwords match', () => {
    const group = new FormGroup({
      password: new FormControl('Test@123'),
      confirmPassword: new FormControl('Test@123'),
    });
    const fn = passwordMatchValidator('password');
    const result = fn(group.get('confirmPassword') as AbstractControl);
    expect(result).toBeNull();
  });

  // TC-39
  it('passwordMatchValidator: should return mismatch error when passwords differ', () => {
    const group = new FormGroup({
      password: new FormControl('Test@123'),
      confirmPassword: new FormControl('Different@1'),
    });
    const fn = passwordMatchValidator('password');
    const result = fn(group.get('confirmPassword') as AbstractControl);
    expect(result).toEqual({ mismatch: true });
  });
});
