import { Component, ElementRef, viewChild, input, effect, inject, DestroyRef, computed } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { formatNumber } from '../../../../core/services/format-number';
import { ChartEmptyState } from '../../../../shared/components/chart-empty-state/chart-empty-state';

Chart.register(...registerables);

@Component({
  selector: 'app-hype-supply-distribution',
  imports: [ChartEmptyState],
  templateUrl: './hype-supply-distribution.html',
  styleUrl: './hype-supply-distribution.css',
})
export class HypeSupplyDistribution {

  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');
  circulatingSupply = input<number | null>(null);
  maxSupply = input<number | null>(null);

  private chart: Chart | undefined;

   // Création de destroyer 
   private readonly destroyRef = inject(DestroyRef);

  constructor() {
    // On dit à Angular de détruire le graphique Chart.js (qui n'est pas uniquement lié au composant) à la fermeture du composant
    this.destroyRef.onDestroy(() => {
      this.chart?.destroy();
    });
    effect(() => {
      const canvas = this.chartCanvas();
      const data = this.supplyData();

      if (!canvas || !this.hasData() || data === null) {
        this.chart?.destroy();
        this.chart = undefined;
        return;
      }

      if (!this.chart) {
        this.createChart(canvas.nativeElement, data);
      } else {
        this.updateChart(this.chart, data);
      }
    });
  }

  hasData = computed(
    () => this.circulatingSupply() !== null && this.maxSupply() !== null,
  );

  // Calcul des données
  supplyData = computed<number[] | null>(() => {
    const circulating = this.circulatingSupply();
    const currentMaxSupply = this.maxSupply();
    if (circulating === null || currentMaxSupply === null) {
      return null;
    }

    const ORIGINAL_TOTAL_SUPPLY = 1000000000;
    const burned = Math.max(0, ORIGINAL_TOTAL_SUPPLY - currentMaxSupply);
    const unissued = Math.max(0, currentMaxSupply - circulating);
    return [circulating, burned, unissued];
  });

  private updateChart(chart: Chart, data: number[]) {
    // Envoi des données à la chart
    chart.data.datasets[0].data = data;
    // Mise à jour de la chart
    chart.update();
  }

  private createChart(canvas: HTMLCanvasElement, data: number[]) {
    const ctx = canvas.getContext('2d');
    if (!ctx) return;


    this.chart = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: [
          'Circulating',
          'Burned',
          'Unissued'
        ],
        datasets: [{
          data: data,
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
