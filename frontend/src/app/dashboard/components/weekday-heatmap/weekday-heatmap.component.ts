import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { WeekdayStat } from '../../../core/models/stats.models';

@Component({
  selector: 'app-weekday-heatmap',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './weekday-heatmap.component.html',
  styleUrls: ['./weekday-heatmap.component.scss'],
})
export class WeekdayHeatmapComponent {
  data = input<WeekdayStat[]>([]);

  cells = computed(() => {
    const stats = this.data();
    const max = Math.max(...stats.map(s => s.orderCount), 1);
    return stats.map(s => ({
      ...s,
      label: s.day.slice(0, 3),
      intensity: s.orderCount / max,
    }));
  });
}
