import { Directive, ElementRef, NgZone, OnDestroy, effect, inject, input } from '@angular/core';

/** Animates a numeric text node from 0 to [countUp]. Usage: <span [countUp]="value" [countUpDuration]="600" [countUpDecimals]="2"> */
@Directive({ selector: '[countUp]', standalone: true })
export class CountUpDirective implements OnDestroy {
  countUp          = input.required<number>();
  countUpDuration  = input(600);
  countUpDecimals  = input(0);

  private readonly el   = inject(ElementRef<HTMLElement>);
  private readonly zone = inject(NgZone);
  private rafId: number | null = null;

  constructor() {
    effect(() => {
      const target   = this.countUp();
      const duration = this.countUpDuration();
      const decimals = this.countUpDecimals();
      this.animate(target, duration, decimals);
    });
  }

  private animate(target: number, duration: number, decimals: number): void {
    if (this.rafId !== null) cancelAnimationFrame(this.rafId);

    const start = performance.now();
    const el    = this.el.nativeElement;

    this.zone.runOutsideAngular(() => {
      const tick = (now: number): void => {
        const progress = Math.min((now - start) / duration, 1);
        el.textContent = (target * progress).toLocaleString('fr-FR', {
          minimumFractionDigits: decimals,
          maximumFractionDigits: decimals,
        });
        if (progress < 1) {
          this.rafId = requestAnimationFrame(tick);
        } else {
          this.rafId = null;
        }
      };
      this.rafId = requestAnimationFrame(tick);
    });
  }

  ngOnDestroy(): void {
    if (this.rafId !== null) cancelAnimationFrame(this.rafId);
  }
}
