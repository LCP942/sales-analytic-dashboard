import { Injectable, signal, effect } from '@angular/core';

/**
 * Manages light/dark theme.
 * Persists preference to localStorage and toggles the 'dark' class
 * on <html> — CSS and Angular Material both key off this class.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly isDark = signal(localStorage.getItem('theme') === 'dark');

  constructor() {
    // effect() runs immediately and on every change — keeps DOM in sync
    effect(() => {
      document.documentElement.classList.toggle('dark', this.isDark());
      localStorage.setItem('theme', this.isDark() ? 'dark' : 'light');
    });
  }

  toggle(): void {
    this.isDark.update(v => !v);
  }
}
