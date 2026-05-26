import { Component, input, viewChild, ElementRef, inject, DestroyRef, effect } from '@angular/core';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-daily-chart',
  templateUrl: './daily-chart.html',
  styleUrl: './daily-chart.css',
})
export class DailyChart {

  private readonly destroyRef = inject(DestroyRef);

  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');

  prices = input<number[]>([]);
  labels = input<number[]>([]);

  private chart: Chart | undefined;

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
          devicePixelRatio: window.devicePixelRatio,
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
                label: (ctx) => `${ctx.parsed.y?.toFixed(2)}$`
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
                stepSize: 0.5,
                precision: 2,
                callback: (val) => val + '$'  
              }
            }
          }
        }
      });
  };
}
