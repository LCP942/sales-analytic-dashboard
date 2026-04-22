import { Component, inject } from '@angular/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { FilterService, DatePreset } from '../../../core/services/filter.service';

@Component({
  selector: 'app-filter-strip',
  standalone: true,
  imports: [MatDatepickerModule, MatNativeDateModule],
  templateUrl: './filter-strip.component.html',
  styleUrls: ['./filter-strip.component.scss'],
})
export class FilterStripComponent {
  protected readonly filter = inject(FilterService);

  readonly presets: { value: DatePreset; label: string }[] = [
    { value: 'today', label: 'Today' },
    { value: '7d',    label: 'Last 7 days' },
    { value: '30d',   label: 'Last 30 days' },
    { value: '12m',   label: 'Last 12 months' },
  ];

  fromDate(): Date { return new Date(this.filter.from()); }
  toDate(): Date   { return new Date(this.filter.to()); }

  onPreset(preset: DatePreset): void {
    this.filter.setPreset(preset);
  }

  onFromChange(date: Date | null): void {
    if (!date) return;
    const iso = this.toIso(date);
    if (iso <= this.filter.to()) this.filter.setCustomRange(iso, this.filter.to());
  }

  onToChange(date: Date | null): void {
    if (!date) return;
    const iso = this.toIso(date);
    if (this.filter.from() <= iso) this.filter.setCustomRange(this.filter.from(), iso);
  }

  private toIso(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
