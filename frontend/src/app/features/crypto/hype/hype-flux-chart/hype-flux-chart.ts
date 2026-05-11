import { Component, input, ViewChild, ElementRef, AfterViewInit, effect, HostListener } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { formatNumber } from '../../services/format-number';

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
  historyPrices = input<number[]>([]);

  private chart: Chart | undefined;
  private viewInitialized = false;

  constructor() {
    effect(() => {
      const labels = this.labels();
      if (this.viewInitialized && labels.length > 0) {
        if (!this.chart) {
          this.createChart();
        } else {
          this.updateChart();
        }
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
    this.viewInitialized = true;
  }

  private getSymmetricBounds() {
    const values = this.netFlow();
    const max = values.length > 0 ? Math.max(...values.map(Math.abs)) : 100000;
    return Math.max(max * 1.2, 1000);
  }

  private updateChart() {
    if (!this.chart) return;
    const finalYMax = this.getSymmetricBounds();
    if (this.chart.options.scales?.['y']) {
      this.chart.options.scales['y'].suggestedMax = finalYMax;
      this.chart.options.scales['y'].suggestedMin = -finalYMax;
    }
    this.chart.data.labels = this.labels().map(l => new Date(l - 86400000).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' }));
    if (this.chart.data.datasets[0]) {
      this.chart.data.datasets[0].data = this.netFlow();
    }
    this.chart.update('none');
  }

  private createChart() {
    const canvas = this.chartCanvas?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    if (this.chart) {
      this.chart.destroy();
    }

    const finalYMax = this.getSymmetricBounds();

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.labels().map(l => new Date(l - 86400000).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short' })),
        datasets: [
          {
            label: 'Flow',
            data: this.netFlow(),
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 3,
            pointRadius: 0,
            pointHoverRadius: 6,
            tension: 0.4,
            fill: true,
            yAxisID: 'y'
          },
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { intersect: false, mode: 'index' },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1a1a1a',
            titleColor: '#e5a075',
            borderColor: 'rgba(249, 115, 22, 0.3)',
            borderWidth: 1,
            padding: 14,
            displayColors: false,
            bodySpacing: 8,
            titleMarginBottom: 10,
            callbacks: {
              title: (items) => {
                const date = new Date(this.labels()[items[0].dataIndex] - 86400000);
                return `📅 ${date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}`;
              },
              label: (ctx) => {
                const i = ctx.dataIndex;
                const b = Math.abs(this.burned()[i]);
                const e = this.issued()[i];
                const f = this.netFlow()[i];
                const p = this.historyPrices()[i];
                return [
                  `🔥 Burned: ${formatNumber(b)}`,
                  `📦 Emitted: ${formatNumber(e)}`,
                  `🌊 Net Flow: ${formatNumber(f)}`,
                  `💰 Price: ${formatNumber(p)} $`
                ];
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
            type: 'linear',
            position: 'left',
            suggestedMin: -finalYMax,
            suggestedMax: finalYMax,
            grid: {
              drawOnChartArea: true,
              color: (context) => context.tick.value === 0 ? 'rgba(211, 33, 10, 0.5)' : 'transparent',
              lineWidth: (context) => context.tick.value === 0 ? 3 : 0
            },
            ticks: {
              color: 'rgba(59, 130, 246, 0.6)',
              font: { size: 10 },
              callback: (val) => Number(val).toLocaleString()
            },
          }
        }
      }
    });
  }
}
