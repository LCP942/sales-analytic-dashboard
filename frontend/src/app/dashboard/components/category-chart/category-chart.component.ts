import {
  Component, input, effect, AfterViewInit,
  ElementRef, ViewChild, OnDestroy,
} from '@angular/core';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { CategoryBreakdown } from '../../../core/models/stats.models';

Chart.register(...registerables);

const PALETTE = ['#3b82f6','#6366f1','#8b5cf6','#ec4899','#f97316','#22c55e'];

@Component({
  selector: 'app-category-chart',
  standalone: true,
  templateUrl: './category-chart.component.html',
  styleUrls: ['./category-chart.component.scss'],
})
export class CategoryChartComponent implements AfterViewInit, OnDestroy {
  data = input.required<CategoryBreakdown[]>();

  @ViewChild('canvas') private canvasRef!: ElementRef<HTMLCanvasElement>;
  private chart: Chart | null = null;

  constructor() {
    effect(() => {
      const cats = this.data();
      if (this.chart) {
        this.chart.data.labels = cats.map(c => c.category);
        this.chart.data.datasets[0].data = cats.map(c => c.itemCount);
        this.chart.update();
        this.chart.resize();
      }
    });
  }

  ngAfterViewInit(): void {
    const config: ChartConfiguration<'doughnut'> = {
      type: 'doughnut',
      data: {
        labels: this.data().map(c => c.category),
        datasets: [{
          data: this.data().map(c => c.itemCount),
          backgroundColor: PALETTE,
          borderWidth: 2,
          hoverOffset: 6,
        }],
      },
      options: {
        responsive: true,
        plugins: {
          legend: { position: 'bottom' },
        },
      },
    };
    this.chart = new Chart(this.canvasRef.nativeElement, config);
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }
}
