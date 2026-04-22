import { Component, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatChipsModule, MatChipListboxChange } from '@angular/material/chips';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrderStatus } from '../../../core/models/order.models';

export interface OrderFilters {
  statuses: OrderStatus[];
  search: string;
}

const STATUS_OPTIONS: { value: OrderStatus; label: string }[] = [
  { value: 'PENDING',   label: 'Pending' },
  { value: 'CONFIRMED', label: 'Confirmed' },
  { value: 'SHIPPED',   label: 'Shipped' },
  { value: 'DELIVERED', label: 'Delivered' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

@Component({
  selector: 'app-order-filters',
  standalone: true,
  imports: [ReactiveFormsModule, MatChipsModule],
  templateUrl: './order-filters.component.html',
  styleUrls: ['./order-filters.component.scss'],
})
export class OrderFiltersComponent {
  filtersChange = output<OrderFilters>();

  readonly statusOptions = STATUS_OPTIONS;

  private selectedStatuses = signal<OrderStatus[]>([]);
  chipValues = signal<string[]>(['ALL']);

  searchControl = new FormControl('');

  constructor() {
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntilDestroyed(),
    ).subscribe(() => this.emit());
  }

  reset(): void {
    this.selectedStatuses.set([]);
    this.chipValues.set(['ALL']);
    this.searchControl.setValue('', { emitEvent: false });
    this.emit();
  }

  onChipChange(event: MatChipListboxChange): void {
    const newValues: string[] = event.value ?? [];
    const hadAll = this.chipValues().includes('ALL');

    if (!hadAll && newValues.includes('ALL')) {
      // User explicitly clicked "All" → reset
      this.selectedStatuses.set([]);
      this.chipValues.set(['ALL']);
    } else {
      // Remove "All" carry-over, keep only real status values
      const statuses = newValues.filter(v => v !== 'ALL') as OrderStatus[];
      if (statuses.length === 0) {
        // Everything deselected → fall back to "All"
        this.selectedStatuses.set([]);
        this.chipValues.set(['ALL']);
      } else {
        this.selectedStatuses.set(statuses);
        this.chipValues.set(statuses);
      }
    }

    this.emit();
  }

  private emit(): void {
    this.filtersChange.emit({
      statuses: this.selectedStatuses(),
      search: this.searchControl.value ?? '',
    });
  }
}
