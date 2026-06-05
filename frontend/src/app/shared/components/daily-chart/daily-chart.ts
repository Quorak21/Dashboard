import { Component, input, viewChild, ElementRef, inject, DestroyRef, effect, computed } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { ChartEmptyState } from '../chart-empty-state/chart-empty-state';

Chart.register(...registerables);

@Component({
  selector: 'app-daily-chart',
  imports: [ChartEmptyState],
  templateUrl: './daily-chart.html',
  styleUrl: './daily-chart.css',
})
export class DailyChart {

  private readonly destroyRef = inject(DestroyRef);

  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');

  prices = input<number[]>([]);
  labels = input<number[]>([]);
  currency = input<string>('');

  hasData = computed(() => this.labels().length > 0);

  private chart: Chart | undefined;

  constructor() {
    // Détruire la chart quand le composant est détruit pour éviter les fuites de mémoire
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
    this.chart.data.labels = this.labels().map(l => new Date(l).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
    this.chart.data.datasets[0].data = this.prices();
    this.chart.update('none');
  }

  private createChart() {
      const canvas = this.chartCanvas()?.nativeElement;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const gradient = ctx.createLinearGradient(0, 0, 0, canvas.clientHeight);
      gradient.addColorStop(0, 'rgba(229, 160, 117, 0.2)');
      gradient.addColorStop(1, 'rgba(229, 160, 117, 0)');

      this.chart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: this.labels().map(l => new Date(l).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })),
          datasets: [{
            data: this.prices(),
            borderColor: '#e5a075',
            backgroundColor: gradient,
            fill: true,
            tension: 0.3,
            pointRadius: 0,
            pointHoverRadius: 4,
            pointHoverBackgroundColor: '#e5a075',
            pointHoverBorderColor: '#fff',
            pointHoverBorderWidth: 1,
            borderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          layout: { padding: { right: 5 } },
          interaction: { intersect: false, mode: 'index' },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: '#1a1a1a',
              titleColor: '#e5a075',
              borderColor: 'rgba(184, 115, 51, 0.3)',
              borderWidth: 1,
              padding: { top: 10, bottom: 10, left: 15, right: 15 },
              titleFont: { size: 14, weight: 'bold' },
              bodyFont: { size: 14 },
              titleAlign: 'center',
              bodyAlign: 'center',
              displayColors: false,
              callbacks: {
                label: (ctx) => `${ctx.parsed.y?.toFixed(2)} ${this.currency()}`
              }
            }
          },
          scales: {
            x: {
              grid: { display: false },
              ticks: {
                color: 'rgba(255, 255, 255, 0.4)',
                maxTicksLimit: 6,
                font: { size: 9 }
              }
            },
            y: {
              position: 'left',
              grid: { color: 'rgba(255, 255, 255, 0.03)' },
              ticks: {
                color: 'rgba(255, 255, 255, 0.4)',
                font: { size: 12 },
                maxTicksLimit: 6,
                callback: (val) => {
                  const num = Number(val);
                  const formatted = num < 10 ? num.toFixed(2) : num.toFixed(0);
                  return formatted + ' ' + this.currency();
                }
              }
            }
          }
        }
      });
  };
}
