import { Component, computed, input } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsCoreOption } from 'echarts/core';
import { TopProduct } from '../../../core/models/stats.models';

const PALETTE = [
  '#3b82f6', '#6366f1', '#8b5cf6', '#a855f7', '#ec4899',
  '#f43f5e', '#f97316', '#eab308', '#22c55e', '#14b8a6',
];

@Component({
  selector: 'app-top-products-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './top-products-chart.component.html',
  styleUrls: ['./top-products-chart.component.scss'],
})
export class TopProductsChartComponent {
  data = input.required<TopProduct[]>();

  protected chartOption = computed<EChartsCoreOption>(() => {
    const products = this.data();
    const names = [...products.map(p => p.name)].reverse();
    const values = [...products.map(p => p.revenue)].reverse();

    return {
      grid: { left: 110, right: 32, top: 12, bottom: 32 },
      tooltip: {
        trigger: 'axis',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        formatter: (p: any) => `<b>${p[0].name}</b><br/>€${Number(p[0].value).toLocaleString('fr-FR')}`,
      },
      xAxis: {
        type: 'value',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        axisLabel: { formatter: (v: any) => `€${(Number(v) / 1000).toFixed(0)}k`, color: '#94a3b8' },
        splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
      },
      yAxis: {
        type: 'category',
        data: names,
        axisLabel: { color: '#64748b' },
        axisLine: { show: false },
        axisTick: { show: false },
      },
      series: [{
        type: 'bar',
        data: values,
        barMaxWidth: 28,
        itemStyle: {
          // eslint-disable-next-line @typescript-eslint/no-explicit-any
          color: (params: any) => PALETTE[params.dataIndex % PALETTE.length],
          borderRadius: [0, 4, 4, 0],
        },
      }],
    };
  });
}
