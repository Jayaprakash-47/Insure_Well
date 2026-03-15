import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-password-strength',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './password-strength.html',
})
export class PasswordStrengthComponent implements OnChanges {
  @Input() password = '';
  strength = 0;
  strengthLabel = '';
  strengthColor = '';
  hint = '';

  ngOnChanges(): void {
    this.calculate();
  }

  private calculate(): void {
    if (!this.password) {
      this.strength = 0;
      return;
    }
    let score = 0;
    const hints: string[] = [];

    if (this.password.length >= 8) score++;
    else hints.push('8+ chars');

    if (/[A-Z]/.test(this.password)) score++;
    else hints.push('uppercase letter');

    if (/[0-9]/.test(this.password)) score++;
    else hints.push('number');

    if (/[^A-Za-z0-9]/.test(this.password)) score++;
    else hints.push('special character');

    this.strength = score;
    this.hint = hints.length ? 'needs ' + hints[0] : '';

    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    const colors = ['', '#ef4444', '#f59e0b', '#3b82f6', '#22c55e'];
    this.strengthLabel = labels[score];
    this.strengthColor = colors[score];
  }
}
