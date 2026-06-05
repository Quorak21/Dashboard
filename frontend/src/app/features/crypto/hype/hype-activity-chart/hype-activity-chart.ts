import { Component, input, viewChild, ElementRef, effect, inject, DestroyRef, computed } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { formatNumber } from '../../../../core/services/format-number';
import { ChartEmptyState } from '../../../../shared/components/chart-empty-state/chart-empty-state';

Chart.register(...registerables);

@Component({
  selector: 'app-hype-activity-chart',
  imports: [ChartEmptyState],
  templateUrl: './hype-activity-chart.html',
  styleUrl: './hype-activity-chart.css',
})
export class HypeActivityChart {
  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');
  private readonly destroyRef = inject(DestroyRef);

  volume = input<number[]>([]);
  openInterest = input<number[]>([]);
  labels = input<number[]>([]);

  hasData = computed(() => this.labels().length > 0);

  private chart: Chart | undefined;

  constructor() {
    this.destroyRef.onDestroy(() => {
      this.chart?.destroy();
    });
    effect(() => {
      const canvas = this.chartCanvas();
      if (!canvas || !this.hasData()) {
        this.chart?.destroy();
        this.chart = undefined;
        return;
      }

      if (!this.chart) {
        this.createChart();
      } else {
        this.updateChart();
      }
    });
  }

  private formatDateLabels() {
    return this.labels().map(l =>
      new Date(l).toLocaleDateString('en-US', { day: 'numeric', month: 'short' }),
    );
  }

  private updateChart() {
    if (!this.chart) return;
    this.chart.data.labels = this.formatDateLabels();
    if (this.chart.data.datasets[0]) {
      this.chart.data.datasets[0].data = this.volume();
    }
    if (this.chart.data.datasets[1]) {
      this.chart.data.datasets[1].data = this.openInterest();
    }
    this.chart.update('none');
  }

  private createChart() {
    const canvas = this.chartCanvas()?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.formatDateLabels(),
        datasets: [
          {
            label: 'Volume',
            data: this.volume(),
            yAxisID: 'y',
            borderColor: '#b87333',
            backgroundColor: 'rgba(184, 115, 51, 0.1)',
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 5,
            tension: 0.3,
          },
          {
            label: 'Open Interest',
            data: this.openInterest(),
            yAxisID: 'y1',
            borderColor: '#f97316',
            backgroundColor: 'rgba(249, 115, 22, 0.08)',
            borderWidth: 2,
            pointRadius: 0,
            pointHoverRadius: 5,
            tension: 0.3,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { intersect: false, mode: 'index' },
        plugins: {
          legend: {
            display: true,
            position: 'top',
            align: 'end',
            labels: {
              color: 'rgba(255, 255, 255, 0.7)',
              boxWidth: 12,
              boxHeight: 2,
              font: { size: 11 },
            },
          },
          tooltip: {
            backgroundColor: '#1a1a1a',
            titleColor: '#e5a075',
            bodyColor: '#f3f4f6',
            borderColor: 'rgba(249, 115, 22, 0.3)',
            borderWidth: 1,
            padding: 14,
            displayColors: true,
            callbacks: {
              title: items => {
                const date = new Date(this.labels()[items[0].dataIndex]);
                return date.toLocaleDateString('en-US', {
                  day: 'numeric',
                  month: 'long',
                  year: 'numeric',
                });
              },
              label: ctx => {
                const value = ctx.parsed.y;
                if (value === null) return `${ctx.dataset.label}: -`;
                return `${ctx.dataset.label}: ${formatNumber(value)} $`;
              },
            },
          },
        },
        scales: {
          x: {
            grid: { display: false },
            ticks: { color: 'rgba(255, 255, 255, 0.4)', font: { size: 10 }, maxTicksLimit: 12 },
          },
          y: {
            type: 'linear',
            position: 'left',
            grid: { color: 'rgba(255, 255, 255, 0.05)' },
            ticks: {
              color: 'rgba(184, 115, 51, 0.8)',
              font: { size: 10 },
              callback: val => Number(val).toLocaleString(),
            },
          },
          y1: {
            type: 'linear',
            position: 'right',
            grid: { drawOnChartArea: false },
            ticks: {
              color: 'rgba(249, 115, 22, 0.8)',
              font: { size: 10 },
              callback: val => Number(val).toLocaleString(),
            },
          },
        },
      },
    });
  }
}
