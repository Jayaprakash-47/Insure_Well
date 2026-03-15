// FILE: src/app/core/services/dark-mode.service.ts

import { Injectable, Renderer2, RendererFactory2 } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

const STORAGE_KEY = 'hs_dark_mode';

@Injectable({ providedIn: 'root' })
export class DarkModeService {

  private renderer: Renderer2;
  private _isDark$ = new BehaviorSubject<boolean>(false);
  isDark$ = this._isDark$.asObservable();

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.init();
  }

  private init(): void {
    // 1. Check localStorage first
    const stored = localStorage.getItem(STORAGE_KEY);
    if (stored !== null) {
      this.apply(stored === 'true');
      return;
    }
    // 2. Fall back to OS preference
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    this.apply(prefersDark);
  }

  toggle(): void {
    this.apply(!this._isDark$.value);
  }

  get isDark(): boolean {
    return this._isDark$.value;
  }

  private apply(dark: boolean): void {
    this._isDark$.next(dark);
    localStorage.setItem(STORAGE_KEY, String(dark));

    if (dark) {
      this.renderer.addClass(document.body, 'dark-mode');
    } else {
      this.renderer.removeClass(document.body, 'dark-mode');
    }
  }
}
