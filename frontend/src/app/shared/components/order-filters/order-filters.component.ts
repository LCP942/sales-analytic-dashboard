import { Component, computed, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatChipsModule, MatChipListboxChange } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { debounceTime, distinctUntilChanged, merge } from 'rxjs';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { OrderStatus } from '../../../core/models/order.models';
import { toLocalIso, yearAgo, today } from '../../../core/utils/date.utils';

export interface OrderFilters {
  statuses: OrderStatus[];
  search: string;
  customer: string;
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

function parseIso(iso: string): Date {
  const [y, m, d] = iso.split('-').map(Number);
  return new Date(y, m - 1, d);
}

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

  selectedCategories = signal<string[]>([]);
  selectedProduct    = signal<string>('');
  selectedCustomer   = signal<string>('');

  private readonly searchValue  = toSignal(this.searchControl.valueChanges,    { initialValue: '' });
  private readonly minValue     = toSignal(this.minAmountControl.valueChanges, { initialValue: null as number | null });
  private readonly maxValue     = toSignal(this.maxAmountControl.valueChanges, { initialValue: null as number | null });

  private readonly defaultFrom = yearAgo();
  private readonly defaultTo   = today();

  isDefault = computed(() =>
    this.chipValues().length === 1 && this.chipValues()[0] === 'ALL'
    && (this.searchValue()  ?? '') === ''
    && this.minValue()  === null
    && this.maxValue()  === null
    && toLocalIso(this.fromDate()) === this.defaultFrom
    && toLocalIso(this.toDate())   === this.defaultTo
    && this.selectedCategories().length === 0
    && this.selectedProduct()  === ''
    && this.selectedCustomer() === ''
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
    this.selectedCustomer.set('');
    this.emit();
  }

  applyExternalFilters(category: string, product: string, customer = ''): void {
    if (category) this.selectedCategories.set([category]);
    if (product)  this.selectedProduct.set(product);
    if (customer) this.selectedCustomer.set(customer);
    this.emit();
  }

  clearCustomer(): void {
    this.selectedCustomer.set('');
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
      search:     this.searchControl.value ?? '',
      customer:   this.selectedCustomer(),
      product:    this.selectedProduct(),
      minAmount:  this.minAmountControl.value,
      maxAmount:  this.maxAmountControl.value,
      from:       toLocalIso(this.fromDate()),
      to:         toLocalIso(this.toDate()),
      categories: this.selectedCategories(),
    });
  }
}
