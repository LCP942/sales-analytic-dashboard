import { Component, computed, input } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsCoreOption } from 'echarts/core';
import { CategoryBreakdown } from '../../../core/models/stats.models';

const PALETTE = ['#3b82f6', '#6366f1', '#8b5cf6', '#ec4899', '#f97316', '#22c55e'];

@Component({
  selector: 'app-category-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './category-chart.component.html',
  styleUrls: ['./category-chart.component.scss'],
})
export class CategoryChartComponent {
  data = input.required<CategoryBreakdown[]>();

  protected chartOption = computed<EChartsCoreOption>(() => {
    const cats = this.data();
    return {
      color: PALETTE,
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { bottom: 0, textStyle: { color: '#64748b' } },
      series: [{
        type: 'pie',
        radius: ['38%', '68%'],
        center: ['50%', '45%'],
        itemStyle: { borderRadius: 6, borderWidth: 2, borderColor: '#fff' },
        label: { show: false },
        emphasis: { label: { show: true, fontWeight: 'bold', fontSize: 13 } },
        data: cats.map(c => ({ name: c.category, value: c.itemCount })),
      }],
    };
  });
}
