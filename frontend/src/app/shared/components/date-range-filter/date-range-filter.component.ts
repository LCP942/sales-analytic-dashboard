import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { FilterService, DatePreset } from '../../../core/services/filter.service';

@Component({
  selector: 'app-date-range-filter',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonToggleModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
    MatNativeDateModule,
  ],
  templateUrl: './date-range-filter.component.html',
})
export class DateRangeFilterComponent {
  protected readonly filter = inject(FilterService);

  fromDate(): Date {
    return new Date(this.filter.from());
  }

  toDate(): Date {
    return new Date(this.filter.to());
  }

  onPreset(preset: DatePreset): void {
    this.filter.setPreset(preset);
  }

  onCustomRange(from: Date | null, to: Date | null): void {
    if (!from || !to) return;
    this.filter.setCustomRange(this.toIso(from), this.toIso(to));
  }

  private toIso(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
