import { Component, input, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { KpiMetrics } from '../../../core/models/stats.models';
import { CountUpDirective } from '../../../shared/directives/count-up.directive';

interface KpiCard {
  label: string;
  numericValue: number;
  decimals: number;
  prefix: string;
  growth: number;
  icon: string;
}

@Component({
  selector: 'app-kpi-cards',
  standalone: true,
  imports: [CommonModule, DecimalPipe, CountUpDirective],
  templateUrl: './kpi-cards.component.html',
})
export class KpiCardsComponent {
  kpis = input.required<KpiMetrics>();

  /** Derived view model — recomputes only when kpis signal changes */
  cards = computed<KpiCard[]>(() => {
    const k = this.kpis();
    return [
      { label: 'Total Revenue',    numericValue: k.revenue,       decimals: 2, prefix: '€', growth: k.revenueGrowth, icon: 'euro' },
      { label: 'Total Orders',     numericValue: k.orderCount,    decimals: 0, prefix: '',  growth: k.orderGrowth,   icon: 'shopping_cart' },
      { label: 'Avg. Order Value', numericValue: k.avgOrderValue, decimals: 2, prefix: '€', growth: k.avgOrderGrowth, icon: 'receipt' },
    ];
  });
}
