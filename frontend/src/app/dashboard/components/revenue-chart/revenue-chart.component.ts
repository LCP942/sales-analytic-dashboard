import { Component, computed, input } from '@angular/core';
import { NgxEchartsDirective } from 'ngx-echarts';
import type { EChartsCoreOption } from 'echarts/core';
import { DataPoint } from '../../../core/models/stats.models';

@Component({
  selector: 'app-revenue-chart',
  standalone: true,
  imports: [NgxEchartsDirective],
  templateUrl: './revenue-chart.component.html',
  styleUrls: ['./revenue-chart.component.scss'],
})
export class RevenueChartComponent {
  data = input.required<DataPoint[]>();

  protected chartOption = computed<EChartsCoreOption>(() => {
    const points = this.data();
    return {
      animationEasingUpdate: 'cubicOut',
      grid: { left: 58, right: 16, top: 12, bottom: 32 },
      tooltip: {
        trigger: 'axis',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        formatter: (p: any) => `<b>${p[0].name}</b><br/>€${Number(p[0].value).toLocaleString('fr-FR')}`,
      },
      xAxis: {
        type: 'category',
        data: points.map(p => p.label),
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: { color: '#94a3b8', fontSize: 11 },
      },
      yAxis: {
        type: 'value',
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        axisLabel: { formatter: (v: any) => `€${(Number(v) / 1000).toFixed(0)}k`, color: '#94a3b8' },
        splitLine: { lineStyle: { type: 'dashed', color: '#e2e8f0' } },
      },
      series: [{
        type: 'line',
        data: points.map(p => p.revenue),
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        itemStyle: { color: '#3b82f6' },
        lineStyle: { color: '#3b82f6', width: 2 },
        areaStyle: {
          color: {
            type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(59,130,246,0.25)' },
              { offset: 1, color: 'rgba(59,130,246,0)' },
            ],
          },
        },
      }],
    };
  });
}
