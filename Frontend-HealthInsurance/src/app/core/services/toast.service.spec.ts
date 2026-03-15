import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ToastService, Toast } from './toast.service';

describe('ToastService', () => {
  let service: ToastService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ToastService);
  });

  // TC-1
  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // TC-2
  it('should add a toast when show() is called', () => {
    service.show('Hello', 'info');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts.length).toBe(1);
    expect(toasts[0].message).toBe('Hello');
    expect(toasts[0].type).toBe('info');
  });

  // TC-3
  it('success() should add a toast with type success', () => {
    service.success('Operation done');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts[0].type).toBe('success');
    expect(toasts[0].message).toBe('Operation done');
  });

  // TC-4
  it('error() should add a toast with type error', () => {
    service.error('Something went wrong');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts[0].type).toBe('error');
  });

  // TC-5
  it('warning() should add a toast with type warning', () => {
    service.warning('Be careful');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts[0].type).toBe('warning');
  });

  // TC-6
  it('dismiss() should remove the toast with the matching id', () => {
    service.show('First', 'info');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    const id = toasts[0].id;
    service.dismiss(id);
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts.length).toBe(0);
  });

  // TC-7
  it('should assign incrementing ids to toasts', () => {
    service.show('A', 'info');
    service.show('B', 'info');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts[0].id).toBeLessThan(toasts[1].id);
  });

  // TC-8
  it('should auto-dismiss a toast after the given duration', fakeAsync(() => {
    service.show('Auto', 'info', 1000);
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts.length).toBe(1);
    tick(1000);
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts.length).toBe(0);
  }));

  // TC-9
  it('multiple toasts can exist simultaneously', () => {
    service.success('One');
    service.error('Two');
    service.warning('Three');
    let toasts: Toast[] = [];
    service.toasts$.subscribe((t) => (toasts = t));
    expect(toasts.length).toBe(3);
  });

  // TC-10
  it('dismiss of non-existent id should not throw', () => {
    expect(() => service.dismiss(9999)).not.toThrow();
  });
});
