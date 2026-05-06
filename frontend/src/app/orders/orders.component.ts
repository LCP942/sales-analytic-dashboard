import { Component, AfterViewInit, DestroyRef, OnInit, ViewChild, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { MatTableModule } from '@angular/material/table';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRippleModule } from '@angular/material/core';

import { OrdersService } from './services/orders.service';
import { OrderSummary } from '../core/models/order.models';
import { toLocalIso, yearAgo } from '../core/utils/date.utils';
import { StatusBadgeComponent } from '../shared/components/status-badge/status-badge.component';
import { OrderFiltersComponent, OrderFilters } from '../shared/components/order-filters/order-filters.component';

@Component({
  selector: 'app-orders',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatRippleModule,
    MatProgressBarModule,
    StatusBadgeComponent,
    OrderFiltersComponent,
  ],
  templateUrl: './orders.component.html',
  styleUrls: ['./orders.component.scss'],
})
export class OrdersComponent implements OnInit, AfterViewInit {
  @ViewChild(OrderFiltersComponent) private filtersRef!: OrderFiltersComponent;

  private readonly ordersService = inject(OrdersService);
  private readonly router        = inject(Router);
  private readonly route         = inject(ActivatedRoute);
  private readonly destroyRef    = inject(DestroyRef);

  readonly columns = ['id', 'orderDate', 'customerName', 'status', 'totalAmount', 'actions'];
  pageSize = signal(10);

  orders        = signal<OrderSummary[]>([]);
  totalElements = signal(0);
  pageIndex     = signal(0);
  loading       = signal(false);
  hasLoaded     = signal(false);
  exporting     = signal(false);

  private activeFilters: OrderFilters = {
    statuses:   [],
    search:     '',
    customer:   '',
    minAmount:  null,
    maxAmount:  null,
    from:       yearAgo(),
    to:         toLocalIso(new Date()),
    categories: [],
    product:    '',
  };

  private activeSort = 'orderDate,desc';

  private preloadCategory = '';
  private preloadProduct  = '';
  private preloadCustomer = '';

  ngOnInit(): void {
    const params = this.route.snapshot.queryParams;
    this.preloadCategory = params['category'] ?? '';
    this.preloadProduct  = params['product']  ?? '';
    this.preloadCustomer = params['customer'] ?? '';

    if (this.preloadCategory) this.activeFilters.categories = [this.preloadCategory];
    if (this.preloadProduct)  this.activeFilters.product    = this.preloadProduct;
    if (this.preloadCustomer) this.activeFilters.customer   = this.preloadCustomer;

    this.load();
  }

  ngAfterViewInit(): void {
    if (this.preloadCategory || this.preloadProduct || this.preloadCustomer) {
      this.filtersRef.applyExternalFilters(this.preloadCategory, this.preloadProduct, this.preloadCustomer);
    }
  }

  onFiltersChange(filters: OrderFilters): void {
    this.activeFilters = filters;
    this.pageIndex.set(0);
    this.load();
  }

  onSort(sort: Sort): void {
    this.activeSort = sort.active && sort.direction ? `${sort.active},${sort.direction}` : 'orderDate,desc';
    this.pageIndex.set(0);
    this.load();
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }

  openOrder(id: number): void {
    this.router.navigate(['/orders', id]);
  }

  createOrder(): void {
    this.router.navigate(['/orders/new']);
  }

  clearFilters(): void {
    this.filtersRef.reset();
  }

  exportCsv(): void {
    if (this.exporting() || this.totalElements() === 0) return;
    this.exporting.set(true);
    const f = this.activeFilters;
    this.ordersService.getOrders(
      f.from, f.to,
      f.customer || f.search, f.statuses,
      0, this.totalElements(),
      this.activeSort,
      f.minAmount, f.maxAmount, f.categories, f.product,
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: page => {
        const escape = (v: unknown) => `"${String(v).replace(/"/g, '""')}"`;
        const headers = ['ID', 'Date', 'Customer', 'Status', 'Total (EUR)'];
        const rows = page.content.map(o => [o.id, o.orderDate, o.customerName, o.status, o.totalAmount]);
        const csv = [headers, ...rows].map(r => r.map(escape).join(',')).join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `orders-${f.from}-to-${f.to}.csv`;
        a.click();
        URL.revokeObjectURL(url);
        this.exporting.set(false);
      },
      error: () => this.exporting.set(false),
    });
  }

  private load(): void {
    this.loading.set(true);
    const f = this.activeFilters;
    this.ordersService.getOrders(
      f.from, f.to,
      f.customer || f.search, f.statuses,
      this.pageIndex(), this.pageSize(),
      this.activeSort,
      f.minAmount, f.maxAmount, f.categories, f.product,
    ).pipe(
      takeUntilDestroyed(this.destroyRef),
    ).subscribe({
      next: page => {
        this.orders.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
        this.hasLoaded.set(true);
      },
      error: () => this.loading.set(false),
    });
  }
}
