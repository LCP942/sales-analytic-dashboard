import { Injectable, signal, computed } from '@angular/core';

/**
 * Tracks in-flight HTTP requests via a counter.
 * The interceptor increments on request start and decrements on response/error.
 */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly _count = signal(0);

  readonly isLoading = computed(() => this._count() > 0);

  increment(): void {
    this._count.update(n => n + 1);
  }

  decrement(): void {
    this._count.update(n => Math.max(0, n - 1));
  }
}
