import {
  Component, input, effect, AfterViewInit,
  ElementRef, ViewChild, OnDestroy,
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { DataPoint } from '../../../core/models/stats.models';

Chart.register(...registerables);

@Component({
  selector: 'app-revenue-chart',
  standalone: true,
  templateUrl: './revenue-chart.component.html',
  styleUrls: ['./revenue-chart.component.scss'],
})
export class RevenueChartComponent implements AfterViewInit, OnDestroy {
  data = input.required<DataPoint[]>();

  @ViewChild('canvas') private canvasRef!: ElementRef<HTMLCanvasElement>;
  private chart: Chart | null = null;

  constructor() {
    // effect() reacts whenever the data signal changes (including after initial load)
    effect(() => {
      const points = this.data();
      if (this.chart) {
        this.chart.data.labels = points.map(p => p.label);
        this.chart.data.datasets[0].data = points.map(p => p.revenue);
        this.chart.update();
        this.chart.resize();
      }
    });
  }

  ngAfterViewInit(): void {
    const config: ChartConfiguration<'line'> = {
      type: 'line',
      data: {
        labels: this.data().map(p => p.label),
        datasets: [{
          label: 'Revenue (€)',
          data: this.data().map(p => p.revenue),
          borderColor: '#3b82f6',
          backgroundColor: 'rgba(59,130,246,0.1)',
          borderWidth: 2,
          pointRadius: 3,
          fill: true,
          tension: 0.4,
        }],
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          y: {
            beginAtZero: false,
            ticks: { callback: v => `€${Number(v).toLocaleString('fr-FR')}` },
          },
        },
      },
    };
    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
