import { Component, Input, ViewChild, ElementRef, AfterViewInit, OnChanges, SimpleChanges } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { formatNumber } from '../../../../core/services/format-number';

Chart.register(...registerables);

@Component({
  selector: 'app-hype-supply-distribution',
  imports: [],
  templateUrl: './hype-supply-distribution.html',
  styleUrl: './hype-supply-distribution.css',
})
export class HypeSupplyDistribution implements AfterViewInit, OnChanges {

  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;
  @Input() circulatingSupply: string = '';
  @Input() maxSupply: string = '';
  @Input() hypeBurned100: string = '';

  private chart: Chart | undefined;

  ngAfterViewInit() {
    this.createChart();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (this.chart) {
      this.updateChart();
    }
  }

  private updateChart() {
    if (!this.chart) return;

    const ORIGINAL_TOTAL_SUPPLY = 1000000000;
    const currentMaxSupply = parseFloat(this.maxSupply) || ORIGINAL_TOTAL_SUPPLY;
    const circulating = parseFloat(this.circulatingSupply) || 0;

    const burned = Math.max(0, ORIGINAL_TOTAL_SUPPLY - currentMaxSupply);
    const unissued = Math.max(0, currentMaxSupply - circulating);

    this.chart.data.datasets[0].data = [circulating, burned, unissued];
    this.chart.update();
  }

  private createChart() {
    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const ORIGINAL_TOTAL_SUPPLY = 1000000000;
    const currentMaxSupply = parseFloat(this.maxSupply) || ORIGINAL_TOTAL_SUPPLY;
    const circulating = parseFloat(this.circulatingSupply) || 0;

    const burned = Math.max(0, ORIGINAL_TOTAL_SUPPLY - currentMaxSupply);
    const unissued = Math.max(0, currentMaxSupply - circulating);

    this.chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: [
          'Circulating',
          'Burned',
          'Unissued'
        ],
        datasets: [{
          data: [circulating, burned, unissued],
          backgroundColor: [
            'rgba(217, 119, 54, 0.85)', // Vibrant Copper
            'rgba(122, 46, 13, 0.85)',  // Dark Burnt Copper
            'rgba(184, 115, 51, 0.1)'   // Transparent/Hollow Copper
          ],
          borderColor: [
            'rgba(253, 186, 116, 1)',   // Bright highlight
            'rgba(217, 119, 54, 1)',    // Medium highlight
            'rgba(184, 115, 51, 0.4)'   // Subtle highlight
          ],
          borderWidth: 2,
          hoverOffset: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '70%',
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: 'rgba(253, 186, 116, 0.8)',
              padding: 20,
              font: {
                family: "'Inter', sans-serif",
                size: 11
              }
            }
          },
          tooltip: {
            backgroundColor: 'rgba(15, 23, 42, 0.9)',
            titleColor: 'rgba(253, 186, 116, 1)',
            bodyColor: '#f0e5e2ff',
            borderColor: 'rgba(184, 115, 51, 0.3)',
            borderWidth: 1,
            padding: 10,
            callbacks: {
              label: function (context) {
                let label = context.label || '';
                if (label) {
                  label += ': ';
                }
                if (context.parsed !== null) {
                  label += formatNumber(context.parsed) + ' HYPE';
                }
                return label;
              }
            }
          }
        }
      }
    });
  }
}
