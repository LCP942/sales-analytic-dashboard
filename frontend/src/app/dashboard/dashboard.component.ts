import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { combineLatest, switchMap } from 'rxjs';
import { FilterService } from '../core/services/filter.service';
import { DashboardService } from './services/dashboard.service';
import { KpiMetrics, DataPoint, TopProduct, CategoryBreakdown, WeekdayStat } from '../core/models/stats.models';
import { KpiCardsComponent } from './components/kpi-cards/kpi-cards.component';
import { RevenueChartComponent } from './components/revenue-chart/revenue-chart.component';
import { TopProductsChartComponent } from './components/top-products-chart/top-products-chart.component';
import { CategoryChartComponent } from './components/category-chart/category-chart.component';
import { WeekdayHeatmapComponent } from './components/weekday-heatmap/weekday-heatmap.component';
import { FilterStripComponent } from '../shared/components/filter-strip/filter-strip.component';
import { SkeletonLoaderComponent } from '../shared/components/skeleton-loader/skeleton-loader.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    KpiCardsComponent,
    RevenueChartComponent,
    TopProductsChartComponent,
    CategoryChartComponent,
    WeekdayHeatmapComponent,
    FilterStripComponent,
    SkeletonLoaderComponent,
  ],
  templateUrl: './dashboard.component.html',
})
export class DashboardComponent {
  private readonly filter = inject(FilterService);
  private readonly stats = inject(DashboardService);

  kpis = signal<KpiMetrics | null>(null);
  revenueData = signal<DataPoint[]>([]);
  topProducts = signal<TopProduct[]>([]);
  categories = signal<CategoryBreakdown[]>([]);
  weekdayStats = signal<WeekdayStat[]>([]);
  isInitialLoad = signal(true);

  constructor() {
    // combineLatest on Signals converted to Observables — all 5 endpoints reload simultaneously
    // toObservable() requires injection context — constructor satisfies this requirement
    combineLatest([
      toObservable(this.filter.from),
      toObservable(this.filter.to),
    ])
      .pipe(
        switchMap(([from, to]) =>
          combineLatest([
            this.stats.getKpis(from, to),
            this.stats.getRevenueOverTime(from, to),
            this.stats.getTopProducts(from, to),
            this.stats.getOrdersByCategory(from, to),
            this.stats.getOrdersByWeekday(from, to),
          ])
        ),
        takeUntilDestroyed()
      )
      .subscribe({
        next: ([kpis, revenue, products, categories, weekday]) => {
          this.kpis.set(kpis);
          this.revenueData.set(revenue);
          this.topProducts.set(products);
          this.categories.set(categories);
          this.weekdayStats.set(weekday);
          this.isInitialLoad.set(false);
        },
        error: () => this.isInitialLoad.set(false),
      });
  }
}
