import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';

import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatRippleModule } from '@angular/material/core';

import { CustomersService } from '../services/customers.service';
import { CustomerSummary } from '../../core/models/customer.models';

@Component({
  selector: 'app-customers-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatRippleModule,
  ],
  templateUrl: './customers-list.component.html',
  styleUrls: ['./customers-list.component.scss'],
})
export class CustomersListComponent implements OnInit {
  private readonly service    = inject(CustomersService);
  private readonly router     = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  columns       = ['name', 'email', 'city', 'orderCount', 'lifetimeValue', 'actions'];
  pageSize      = 20;

  customers     = signal<CustomerSummary[]>([]);
  totalElements = signal(0);
  pageIndex     = signal(0);
  loading       = signal(false);
  hasLoaded     = signal(false);

  searchName = '';
  private readonly searchSubject = new Subject<void>();

  constructor() {
    this.searchSubject.pipe(
      debounceTime(300),
      takeUntilDestroyed(),
    ).subscribe(() => {
      this.pageIndex.set(0);
      this.load();
    });
  }

  ngOnInit(): void {
    this.load();
  }

  onSearchInput(event: Event): void {
    this.searchName = (event.target as HTMLInputElement).value;
    this.searchSubject.next();
  }

  clearSearch(): void {
    this.searchName = '';
    this.searchSubject.next();
  }

  onSearch(): void {
    this.pageIndex.set(0);
    this.load();
  }

  onPage(e: PageEvent): void {
    this.pageIndex.set(e.pageIndex);
    this.pageSize = e.pageSize;
    this.load();
  }

  openCustomer(id: number): void {
    this.router.navigate(['/customers', id]);
  }

  createCustomer(): void {
    this.router.navigate(['/customers/new']);
  }

  private load(): void {
    this.loading.set(true);
    this.service.getCustomers(this.searchName, this.pageIndex(), this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: page => {
          this.customers.set(page.content);
          this.totalElements.set(page.totalElements);
          this.loading.set(false);
          this.hasLoaded.set(true);
        },
        error: () => this.loading.set(false),
      });
  }
}
