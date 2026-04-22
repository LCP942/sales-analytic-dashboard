import { Directive, ElementRef, input, effect, inject, OnDestroy } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { takeWhile } from 'rxjs/operators';

/**
 * CountUpDirective — animates a numeric text node from 0 to [countUp].
 *
 * Usage:  <span [countUp]="value" [countUpDuration]="600" [countUpDecimals]="2">
 *
 * Uses RxJS interval (not setTimeout) to demonstrate reactive patterns.
 * The effect() re-triggers automatically when the input signal changes
 * (e.g. on date filter change), restarting the animation.
 */
@Directive({ selector: '[countUp]', standalone: true })
export class CountUpDirective implements OnDestroy {
  countUp       = input.required<number>();
  countUpDuration = input(600);   // ms
  countUpDecimals = input(0);

  private readonly el = inject(ElementRef<HTMLElement>);
  private sub: Subscription | null = null;

  constructor() {
    effect(() => {
      const target   = this.countUp();
      const duration = this.countUpDuration();
      const decimals = this.countUpDecimals();
      this.animate(target, duration, decimals);
    });
  }

  private animate(target: number, duration: number, decimals: number): void {
    this.sub?.unsubscribe();

    const steps    = Math.max(1, Math.round(duration / 16)); // ~60fps
    const increment = target / steps;
    let current    = 0;
    let step       = 0;

    this.sub = interval(duration / steps)
      .pipe(takeWhile(() => step < steps))
      .subscribe(() => {
        step++;
        current = step === steps ? target : Math.min(current + increment, target);
        this.el.nativeElement.textContent = current.toLocaleString('fr-FR', {
          minimumFractionDigits: decimals,
          maximumFractionDigits: decimals,
        });
      });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}
