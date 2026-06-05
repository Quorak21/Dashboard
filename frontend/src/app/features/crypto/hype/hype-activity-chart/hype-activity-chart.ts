import { Component, input, viewChild, ElementRef, effect, inject, DestroyRef, computed } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { formatNumber } from '../../../../core/services/format-number';
import { ChartEmptyState } from '../../../../shared/components/chart-empty-state/chart-empty-state';

Chart.register(...registerables);

const VOLUME_COLOR = '#c97b3d';
const OPEN_INTEREST_COLOR = '#5eb3f6';

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

  private formatAxisTick(value: string | number) {
    const num = Number(value);
    if (Number.isNaN(num)) return '';
    return `${formatNumber(num)} $`;
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
            borderColor: VOLUME_COLOR,
            backgroundColor: 'rgba(201, 123, 61, 0.12)',
            borderWidth: 2.5,
            pointRadius: 0,
            pointHoverRadius: 5,
            pointStyle: 'circle',
            tension: 0.45,
            cubicInterpolationMode: 'monotone',
          },
          {
            label: 'Open Interest',
            data: this.openInterest(),
            yAxisID: 'y1',
            borderColor: OPEN_INTEREST_COLOR,
            backgroundColor: 'rgba(94, 179, 246, 0.1)',
            borderWidth: 2.5,
            pointRadius: 0,
            pointHoverRadius: 5,
            pointStyle: 'circle',
            tension: 0.45,
            cubicInterpolationMode: 'monotone',
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
              color: 'rgba(255, 255, 255, 0.75)',
              usePointStyle: true,
              pointStyle: 'circle',
              boxWidth: 8,
              boxHeight: 8,
              padding: 16,
              font: { size: 11 },
            },
          },
          tooltip: {
            backgroundColor: '#1a1a1a',
            titleColor: '#e5a075',
            bodyColor: '#f3f4f6',
            borderColor: 'rgba(255, 255, 255, 0.12)',
            borderWidth: 1,
            padding: 14,
            displayColors: true,
            usePointStyle: true,
            boxPadding: 8,
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
                if (value === null) return `  ${ctx.dataset.label}: -`;
                return `  ${ctx.dataset.label}: ${formatNumber(value)} $`;
              },
              labelColor: ctx => ({
                borderColor: 'transparent',
                backgroundColor: ctx.dataset.borderColor as string,
                borderWidth: 0,
              }),
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
              color: VOLUME_COLOR,
              font: { size: 10 },
              maxTicksLimit: 5,
              callback: val => this.formatAxisTick(val),
            },
          },
          y1: {
            type: 'linear',
            position: 'right',
            grid: { drawOnChartArea: false },
            ticks: {
              color: OPEN_INTEREST_COLOR,
              font: { size: 10 },
              maxTicksLimit: 5,
              callback: val => this.formatAxisTick(val),
            },
          },
        },
      },
    });
  }
}
