import { Component, ElementRef, viewChild, input, effect, inject, DestroyRef } from '@angular/core';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-price-chart',
  templateUrl: './price-chart.html',
  styleUrl: './price-chart.css',
})
export class PriceChart {
  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');
  prices = input<number[]>([]);
  labels = input<number[]>([]);
  currency = input<string>('');

  private chart: Chart | undefined;

  private readonly destroyRef = inject(DestroyRef);

  constructor() {
    // Détruire la chart quand le composant est détruit pour éviter les fuites de mémoire
    this.destroyRef.onDestroy(() => {
      this.chart?.destroy();
    });
    effect(() => {
      const canvas = this.chartCanvas();
      if (canvas && this.labels().length > 0) {
        if (!this.chart) { // Si la chart n'existe pas, on la crée
          this.createChart();
        } else { // Si la chart existe, on la met à jour
          this.updateChart();
        }
      }
    });
  }

  private updateChart() {
    if (!this.chart) return;
    this.chart.data.labels = this.labels().map(l => new Date(l).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }));
    this.chart.data.datasets[0].data = this.prices();
    this.chart.update('none');
  }

  private createChart() {
      const canvas = this.chartCanvas()?.nativeElement;
      if (!canvas) return;
      const ctx = canvas.getContext('2d');
      if (!ctx) return;

      const gradient = ctx.createLinearGradient(0, 0, 0, canvas.clientHeight);
      gradient.addColorStop(0, 'rgba(184, 115, 51, 0.3)');
      gradient.addColorStop(1, 'rgba(184, 115, 51, 0)');

      this.chart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: this.labels().map(l => new Date(l).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })),
          datasets: [{
            data: this.prices(),
            borderColor: '#b87333',
            backgroundColor: gradient,
            fill: true,
            tension: 0.4,
            pointRadius: 0,
            pointHoverRadius: 6,
            pointHoverBackgroundColor: '#b87333',
            pointHoverBorderColor: '#fff',
            pointHoverBorderWidth: 2,
            borderWidth: 3
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          devicePixelRatio: window.devicePixelRatio,
          layout: { padding: { right: 10 } },
          interaction: { intersect: false, mode: 'index' },
          plugins: {
            legend: { display: false },
            tooltip: {
              backgroundColor: '#1a1a1a',
              titleColor: '#e5a075',
              borderColor: 'rgba(184, 115, 51, 0.3)',
              borderWidth: 1,
              padding: { top: 10, bottom: 10, left: 20, right: 20 },
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
                maxTicksLimit: 8,
                font: { size: 10 }
              }
            },
            y: {
              position: 'left',
              grid: { color: 'rgba(255, 255, 255, 0.05)' },
              ticks: {
                color: 'rgba(255, 255, 255, 0.4)',
                font: { size: 10 },
                callback: (val) => Number(val)
              }
            }
          }
        }
      });
    };
}
