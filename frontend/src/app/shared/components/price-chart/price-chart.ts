import { Component, Input, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges, HostListener } from '@angular/core';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-price-chart',
  standalone: true,
  templateUrl: './price-chart.html',
  styleUrl: './price-chart.css',
})
export class PriceChart implements AfterViewInit, OnChanges {
  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;
  @Input() prices: number[] = [];
  @Input() labels: number[] = [];

  private chart: Chart | undefined;

  @HostListener('window:resize')
  onResize() {
    if (this.chart) {
      this.chart.resize();
    }
  }

  ngAfterViewInit() {
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.chart && (changes['prices'] || changes['labels'])) {
      this.updateChart();
    }
  }

  private createChart() {
    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const gradient = ctx.createLinearGradient(0, 0, 0, this.chartCanvas.nativeElement.clientHeight);
    gradient.addColorStop(0, 'rgba(184, 115, 51, 0.3)');
    gradient.addColorStop(1, 'rgba(184, 115, 51, 0)');

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.labels.map(l => new Date(l).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' })),
        datasets: [{
          data: this.prices,
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
        layout: { padding: { right: 10 } },
        interaction: { intersect: false, mode: 'index' },
        plugins: {
          legend: { display: false },
          tooltip: {
            backgroundColor: '#1a1a1a',
            titleColor: '#e5a075',
            borderColor: 'rgba(184, 115, 51, 0.3)',
            borderWidth: 1,
            padding: 14,
            titleFont: { size: 18, weight: 'bold' },
            bodyFont: { size: 16 },
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
              callback: (val) => '$' + Number(val)
            }
          }
        }
      }
    });
  }

  private updateChart() {
    if (!this.chart) return;
    this.chart.data.labels = this.labels.map(l => new Date(l).toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' }));
    this.chart.data.datasets[0].data = this.prices;
    this.chart.update();
  }
}
