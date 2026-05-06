import { Component, input, ViewChild, ElementRef, AfterViewInit, effect, HostListener } from '@angular/core';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-hype-flux-chart',
  standalone: true,
  templateUrl: './hype-flux-chart.html',
  styleUrl: './hype-flux-chart.css',
})
export class HypeFluxChart implements AfterViewInit {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;

  burned = input<number[]>([]);
  issued = input<number[]>([]);
  netFlow = input<number[]>([]);
  labels = input<number[]>([]);

  private chart: Chart | undefined;

  constructor() {
    // Effet pour mettre à jour le graphique quand les signaux changent
    effect(() => {
      if (this.chart) {
        this.updateChart();
      }
    });
  }

  @HostListener('window:resize')
  onResize() {
    if (this.chart) {
      this.chart.resize();
    }
  }

  ngAfterViewInit() {
    this.createChart();
  }

  private createChart() {
    setTimeout(() => {
      const ctx = this.chartCanvas.nativeElement.getContext('2d');
      if (!ctx) return;

      this.chart = new Chart(ctx, {
        type: 'line',
        data: {
          labels: this.labels().map(l => new Date(l).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })),
          datasets: [
            {
              label: 'Net Flow',
              data: this.netFlow(),
              borderColor: '#3b82f6', // Blue
              backgroundColor: 'rgba(59, 130, 246, 0.1)',
              borderWidth: 3,
              pointRadius: 0,
              pointHoverRadius: 6,
              tension: 0.4,
              fill: true
            },
            {
              label: 'Daily Issued',
              data: this.issued(),
              borderColor: '#eab308', // Yellow
              borderWidth: 2,
              pointRadius: 0,
              pointHoverRadius: 4,
              tension: 0.4,
              borderDash: [5, 5] // Pointillés pour différencier
            },
            {
              label: 'Daily Burned',
              data: this.burned().map(b => Math.abs(b)),
              borderColor: '#ef4444', // Red
              borderWidth: 2,
              pointRadius: 0,
              pointHoverRadius: 4,
              tension: 0.4
            }
          ]
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
                color: 'rgba(255, 255, 255, 0.6)',
                usePointStyle: true,
                boxWidth: 8,
                padding: 15,
                font: { size: 10 }
              }
            },
            tooltip: {
              backgroundColor: '#1a1a1a',
              titleColor: '#e5a075',
              borderColor: 'rgba(249, 115, 22, 0.3)',
              borderWidth: 1,
              padding: 12,
              callbacks: {
                label: (ctx) => {
                  const val = Number(ctx.parsed.y);
                  return `${ctx.dataset.label}: ${val.toLocaleString()} HYPE`;
                }
              }
            }
          },
          scales: {
            x: {
              grid: { display: false },
              ticks: { color: 'rgba(255, 255, 255, 0.4)', font: { size: 10 }, maxTicksLimit: 10 }
            },
            y: {
              grid: { color: 'rgba(255, 255, 255, 0.05)' },
              ticks: {
                color: 'rgba(255, 255, 255, 0.4)',
                font: { size: 10 },
                callback: (val) => Number(val).toLocaleString()
              }
            }
          }
        }
      });
    }, 50);
  }

  private updateChart() {
    if (!this.chart) return;
    this.chart.data.labels = this.labels().map(l => new Date(l).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }));
    this.chart.data.datasets[0].data = this.netFlow();
    this.chart.data.datasets[1].data = this.issued();
    this.chart.data.datasets[2].data = this.burned().map(b => Math.abs(b));
    this.chart.update('none');
  }
}
