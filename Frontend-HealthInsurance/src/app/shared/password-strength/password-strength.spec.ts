import { TestBed, ComponentFixture } from '@angular/core/testing';
import { PasswordStrengthComponent } from './password-strength';

describe('PasswordStrengthComponent', () => {
  let fixture: ComponentFixture<PasswordStrengthComponent>;
  let component: PasswordStrengthComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PasswordStrengthComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(PasswordStrengthComponent);
    component = fixture.componentInstance;
  });

  // TC-40
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // TC-41
  it('strength should be 0 for empty password', () => {
    component.password = '';
    component.ngOnChanges();
    expect(component.strength).toBe(0);
  });

  // TC-42
  it('strength should be 1 for only length satisfied', () => {
    component.password = 'abcdefgh'; // 8 chars, no uppercase, no number, no special
    component.ngOnChanges();
    expect(component.strength).toBe(1);
  });

  // TC-43
  it('strength should be 4 for a strong password', () => {
    component.password = 'StrongPass@1';
    component.ngOnChanges();
    expect(component.strength).toBe(4);
    expect(component.strengthLabel).toBe('Strong');
  });

  // TC-44
  it('strength should be 2 for password with length and uppercase', () => {
    component.password = 'Abcdefgh'; // length + uppercase
    component.ngOnChanges();
    expect(component.strength).toBe(2);
  });

  // TC-45
  it('hint should mention the first missing criterion', () => {
    component.password = 'abcdefgh'; // missing uppercase, number, special
    component.ngOnChanges();
    expect(component.hint).toContain('needs');
  });

  // TC-46
  it('hint should be empty for a fully strong password', () => {
    component.password = 'StrongPass@1';
    component.ngOnChanges();
    expect(component.hint).toBe('');
  });

  // TC-47
  it('strengthColor should be red (#ef4444) for weak password', () => {
    component.password = 'abcdefgh'; // score = 1 → Weak
    component.ngOnChanges();
    expect(component.strengthColor).toBe('#ef4444');
  });

  // TC-48
  it('strengthColor should be green (#22c55e) for strong password', () => {
    component.password = 'StrongPass@1';
    component.ngOnChanges();
    expect(component.strengthColor).toBe('#22c55e');
  });
});
