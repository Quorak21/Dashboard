import { Component, ElementRef, viewChild, input, effect, inject, DestroyRef, computed } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { ChartEmptyState } from '../chart-empty-state/chart-empty-state';
import type { SectorWeight } from '../../../core/models';

Chart.register(...registerables);

const CHART_COLORS = [
  '#b87333', // Primary copper
  '#d4a373', // Light copper
  '#a3704c', // Medium dark copper
  '#8c531d', // Dark copper
  '#e5c158', // Light gold-copper
  '#5c3a21', // Espresso copper
  '#f2d49b', // Sandy copper
];

@Component({
  selector: 'app-etf-sector-chart',
  imports: [ChartEmptyState],
  templateUrl: './etf-sector-chart.html',
  styleUrl: './etf-sector-chart.css',
})
export class EtfSectorChart {
  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');
  sectorWeights = input<SectorWeight[]>([]);

  hasData = computed(() => this.sectorWeights().length > 0);

  private chart: Chart | undefined;

  private readonly destroyRef = inject(DestroyRef);

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

  private updateChart() {
    if (!this.chart) return;
    const data = this.sectorWeights();
    this.chart.data.labels = data.map(sw => sw.sector);
    this.chart.data.datasets[0].data = data.map(sw => sw.weightPercent);
    this.chart.data.datasets[0].backgroundColor = data.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]);
    this.chart.update('none');
  }

  private createChart() {
    const canvas = this.chartCanvas()?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const data = this.sectorWeights();

    this.chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: data.map(sw => sw.sector),
        datasets: [{
          data: data.map(sw => sw.weightPercent),
          backgroundColor: data.map((_, i) => CHART_COLORS[i % CHART_COLORS.length]),
          borderColor: '#111111',
          borderWidth: 2,
          hoverOffset: 4,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: window.innerWidth < 768 ? 'bottom' : 'right',
            labels: {
              color: 'rgba(255, 255, 255, 0.7)',
              font: {
                size: 11,
                weight: 'normal',
              },
              padding: 15,
              usePointStyle: true,
              pointStyle: 'circle',
            }
          },
          tooltip: {
            backgroundColor: '#1a1a1a',
            titleColor: '#e5a075',
            borderColor: 'rgba(184, 115, 51, 0.3)',
            borderWidth: 1,
            padding: { top: 8, bottom: 8, left: 16, right: 16 },
            titleFont: { size: 13, weight: 'bold' },
            bodyFont: { size: 13 },
            displayColors: false,
            callbacks: {
              label: (ctx) => ` ${ctx.parsed}%`
            }
          }
        },
        cutout: '65%',
      }
    });
  }
}
