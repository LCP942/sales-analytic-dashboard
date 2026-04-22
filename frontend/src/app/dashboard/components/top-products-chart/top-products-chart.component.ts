import {
  Component, input, effect, AfterViewInit,
  ElementRef, ViewChild, OnDestroy,
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { TopProduct } from '../../../core/models/stats.models';

Chart.register(...registerables);

const PALETTE = [
  '#3b82f6','#6366f1','#8b5cf6','#a855f7','#ec4899',
  '#f43f5e','#f97316','#eab308','#22c55e','#14b8a6',
];

@Component({
  selector: 'app-top-products-chart',
  standalone: true,
  templateUrl: './top-products-chart.component.html',
  styleUrls: ['./top-products-chart.component.scss'],
})
export class TopProductsChartComponent implements AfterViewInit, OnDestroy {
  data = input.required<TopProduct[]>();

  @ViewChild('canvas') private canvasRef!: ElementRef<HTMLCanvasElement>;
  private chart: Chart | null = null;

  constructor() {
    effect(() => {
      const products = this.data();
      if (this.chart) {
        this.chart.data.labels = products.map(p => p.name);
        this.chart.data.datasets[0].data = products.map(p => p.revenue);
        this.chart.update();
        this.chart.resize();
      }
    });
  }

  ngAfterViewInit(): void {
    const config: ChartConfiguration<'bar'> = {
      type: 'bar',
      data: {
        labels: this.data().map(p => p.name),
        datasets: [{
          label: 'Revenue (€)',
          data: this.data().map(p => p.revenue),
          backgroundColor: PALETTE,
          borderRadius: 4,
        }],
      },
      options: {
        indexAxis: 'y',  // horizontal bar
        responsive: true,
        plugins: { legend: { display: false } },
        scales: {
          x: { ticks: { callback: v => `€${Number(v).toLocaleString('fr-FR')}` } },
        },
      },
    };
    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
