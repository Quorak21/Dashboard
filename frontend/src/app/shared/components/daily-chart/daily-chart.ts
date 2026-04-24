import { Component, Input, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges, HostListener } from '@angular/core';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
  selector: 'app-daily-chart',
  standalone: true,
  templateUrl: './daily-chart.html',
  styleUrl: './daily-chart.css',
})
export class DailyChart implements AfterViewInit, OnChanges {
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

    const gradient = ctx.createLinearGradient(0, 0, 0, 250);
    gradient.addColorStop(0, 'rgba(229, 160, 117, 0.2)'); // Light Copper
    gradient.addColorStop(1, 'rgba(229, 160, 117, 0)');

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.labels.map(l => new Date(l).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })),
        datasets: [{
          data: this.prices,
          borderColor: '#e5a075', // Light Copper
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
  }

  private updateChart() {
    if (!this.chart) return;
    this.chart.data.labels = this.labels.map(l => new Date(l).toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' }));
    this.chart.data.datasets[0].data = this.prices;
    this.chart.update('none'); // Update without animation for "live" feel
  }
}
