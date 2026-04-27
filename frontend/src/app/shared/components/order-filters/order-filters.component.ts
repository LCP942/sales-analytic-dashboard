import { Component, computed, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatChipsModule, MatChipListboxChange } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { debounceTime, distinctUntilChanged, merge } from 'rxjs';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { OrderStatus } from '../../../core/models/order.models';

export interface OrderFilters {
  statuses: OrderStatus[];
  search: string;
  product: string;
  minAmount: number | null;
  maxAmount: number | null;
  from: string;
  to: string;
  categories: string[];
}

const STATUS_OPTIONS: { value: OrderStatus; label: string }[] = [
  { value: 'PENDING',   label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SHIPPED',   label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

function toIso(d: Date): string {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function parseIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function yearAgo(): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 1);
  return toIso(d);
}

function today(): string { return toIso(new Date()); }

@Component({
  selector: 'app-order-filters',
  standalone: true,
  imports: [ReactiveFormsModule, MatChipsModule, MatDatepickerModule, MatNativeDateModule],
  templateUrl: './order-filters.component.html',
  styleUrls: ['./order-filters.component.scss'],
})
export class OrderFiltersComponent {
  filtersChange = output<OrderFilters>();

  readonly statusOptions = STATUS_OPTIONS;

  private selectedStatuses = signal<OrderStatus[]>([]);
  chipValues = signal<string[]>(['ALL']);

  searchControl    = new FormControl('');
  minAmountControl = new FormControl<number | null>(null);
  maxAmountControl = new FormControl<number | null>(null);

  fromDate = signal<Date>(parseIso(yearAgo()));
  toDate   = signal<Date>(parseIso(today()));

  // Set by dashboard drill-down, displayed as removable chips.
  selectedCategories = signal<string[]>([]);
  selectedProduct    = signal<string>('');

  // Signals mirroring FormControl values so computed() can track them.
  private readonly searchValue  = toSignal(this.searchControl.valueChanges,    { initialValue: '' });
  private readonly minValue     = toSignal(this.minAmountControl.valueChanges, { initialValue: null as number | null });
  private readonly maxValue     = toSignal(this.maxAmountControl.valueChanges, { initialValue: null as number | null });

  // Stored at construction time so the comparison is stable within a session.
  private readonly defaultFrom = yearAgo();
  private readonly defaultTo   = today();

  isDefault = computed(() =>
    this.chipValues().length === 1 && this.chipValues()[0] === 'ALL'
    && (this.searchValue()  ?? '') === ''
    && this.minValue()  === null
    && this.maxValue()  === null
    && toIso(this.fromDate()) === this.defaultFrom
    && toIso(this.toDate())   === this.defaultTo
    && this.selectedCategories().length === 0
    && this.selectedProduct() === ''
  );

  constructor() {
    merge(
      this.searchControl.valueChanges,
    ).pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe(() => this.emit());

    merge(this.minAmountControl.valueChanges, this.maxAmountControl.valueChanges).pipe(
      debounceTime(400),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe(() => this.emit());
  }

  reset(): void {
    this.selectedStatuses.set([]);
    this.chipValues.set(['ALL']);
    this.searchControl.setValue('', { emitEvent: false });
    this.minAmountControl.setValue(null, { emitEvent: false });
    this.maxAmountControl.setValue(null, { emitEvent: false });
    this.fromDate.set(parseIso(this.defaultFrom));
    this.toDate.set(parseIso(this.defaultTo));
    this.selectedCategories.set([]);
    this.selectedProduct.set('');
    this.emit();
  }

  /** Pre-fills category and product from a dashboard drill-down navigation. */
  applyExternalFilters(category: string, product: string): void {
    if (category) this.selectedCategories.set([category]);
    if (product)  this.selectedProduct.set(product);
    this.emit();
  }

  onChipChange(event: MatChipListboxChange): void {
    const newValues: string[] = event.value ?? [];
    const hadAll = this.chipValues().includes('ALL');

    if (!hadAll && newValues.includes('ALL')) {
      this.selectedStatuses.set([]);
      this.chipValues.set(['ALL']);
    } else {
      const statuses = newValues.filter(v => v !== 'ALL') as OrderStatus[];
      if (statuses.length === 0) {
        this.selectedStatuses.set([]);
        this.chipValues.set(['ALL']);
      } else {
        this.selectedStatuses.set(statuses);
        this.chipValues.set(statuses);
      }
    }
    this.emit();
  }

  removeCategory(cat: string): void {
    this.selectedCategories.update(cs => cs.filter(c => c !== cat));
    this.emit();
  }

  clearProduct(): void {
    this.selectedProduct.set('');
    this.emit();
  }

  onFromChange(date: Date | null): void {
    if (!date) return;
    this.fromDate.set(date);
    this.emit();
  }

  onToChange(date: Date | null): void {
    if (!date) return;
    this.toDate.set(date);
    this.emit();
  }

  private emit(): void {
    this.filtersChange.emit({
      statuses:   this.selectedStatuses(),
      search:     this.searchControl.value    ?? '',
      product:    this.selectedProduct(),
      minAmount:  this.minAmountControl.value,
      maxAmount:  this.maxAmountControl.value,
      from:       toIso(this.fromDate()),
      to:         toIso(this.toDate()),
      categories: this.selectedCategories(),
    });
  }
}
