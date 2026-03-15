// FILE: src/app/shared/dark-mode-toggle/dark-mode-toggle.component.ts

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import { DarkModeService } from '../../core/services/dark-mode.service';

@Component({
  selector: 'app-dark-mode-toggle',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dark-mode-toggle.html',
  styleUrls: ['./dark-mode-toggle.css']
})
export class DarkModeToggle{

  isDark$: Observable<boolean>;

  constructor(public darkMode: DarkModeService) {
    this.isDark$ = this.darkMode.isDark$;
  }

  toggle(): void {
    this.darkMode.toggle();
  }
}
