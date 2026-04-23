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

  fromDate(): Date { return this.parseLocalDate(this.filter.from()); }
  toDate(): Date   { return this.parseLocalDate(this.filter.to()); }

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
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  private parseLocalDate(iso: string): Date {
    const [y, m, d] = iso.split('-').map(Number);
    return new Date(y, m - 1, d);
  }
}
