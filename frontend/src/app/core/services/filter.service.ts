import { Injectable, signal } from '@angular/core';

export type DatePreset = 'today' | '7d' | '30d' | '12m';

function toIso(date: Date): string {
  return date.toISOString().split('T')[0];
}

function today(): string {
  return toIso(new Date());
}

function daysAgo(n: number): string {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return toIso(d);
}

function monthsAgo(n: number): string {
  const d = new Date();
  d.setMonth(d.getMonth() - n);
  return toIso(d);
}

/**
 * Single source of truth for the active date range filter.
 * Uses Angular Signals so any component can react without RxJS boilerplate.
 */
@Injectable({ providedIn: 'root' })
export class FilterService {
  readonly from = signal<string>(daysAgo(29)); // default: last 30 days
  readonly to = signal<string>(today());
  readonly activePreset = signal<DatePreset | null>('30d');

  setPreset(preset: DatePreset): void {
    const t = today();
    const ranges: Record<DatePreset, string> = {
      today: today(),
      '7d':  daysAgo(6),
      '30d': daysAgo(29),
      '12m': monthsAgo(12),
    };
    this.from.set(ranges[preset]);
    this.to.set(t);
    this.activePreset.set(preset);
  }

  setCustomRange(from: string, to: string): void {
    this.from.set(from);
    this.to.set(to);
    this.activePreset.set(null);
  }
}
