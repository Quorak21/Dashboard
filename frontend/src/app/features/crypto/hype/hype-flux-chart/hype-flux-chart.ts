import { Component, input, viewChild, ElementRef, effect, inject, DestroyRef } from '@angular/core';
import { Chart, registerables } from 'chart.js';
import { formatNumber } from '../../../../core/services/format-number';

Chart.register(...registerables);

@Component({
  selector: 'app-hype-flux-chart',
  templateUrl: './hype-flux-chart.html',
  styleUrl: './hype-flux-chart.css',
})
export class HypeFluxChart {

  chartCanvas = viewChild<ElementRef<HTMLCanvasElement>>('chartCanvas');
  // Création de destroyer 
  private readonly destroyRef = inject(DestroyRef);

  // Création des signals avec les données envoyées par le parent
  burned = input<number[]>([]);
  issued = input<number[]>([]);
  netFlow = input<number[]>([]);
  labels = input<number[]>([]);
  historyPrices = input<number[]>([]);

  private chart: Chart | undefined;

  constructor() {
    // On dit à Angular de détruire le graphique Chart.js (qui n'est pas uniquement lié au composant) à la fermeture du composant
    this.destroyRef.onDestroy(() => {
      this.chart?.destroy();
    });
    effect(() => {
      const canvas = this.chartCanvas();
      if (canvas && this.labels().length > 0) {
        if (!this.chart) {
          this.createChart();
        } else {
          this.updateChart();
        }
      }
    });
  }

  // Calcul pour avoir des bornes symétrique et donc avoir le 0 sur l'axe Y au centre
  private getSymmetricBounds() {
    const values = this.netFlow();
    const max = values.length > 0 ? Math.max(...values.map(Math.abs)) : 100000;
    return Math.max(max * 1.025, 1000);
  }
  // L'update de la chart, on renvoie les nouvelles data + nouvel axe Y
  private updateChart() {
    if (!this.chart) return;
    const finalYMax = this.getSymmetricBounds();
    if (this.chart.options.scales?.['y']) {
      this.chart.options.scales['y'].suggestedMax = finalYMax;
      this.chart.options.scales['y'].suggestedMin = -finalYMax;
    }

    // On enleve un jour pour afficher le bon jour
    this.chart.data.labels = this.labels().map(l => new Date(l - 86400000).toLocaleDateString('en-US', { day: 'numeric', month: 'short' }));
    if (this.chart.data.datasets[0]) {
      this.chart.data.datasets[0].data = this.netFlow();
    }
    this.chart.update('none');
  }
  // TODO : Régler ce problème de pic de donnée lors des unlock, mais a voir quand on aura dépasser les 30j de moyenne
  private createChart() {
    const canvas = this.chartCanvas()?.nativeElement;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const finalYMax = this.getSymmetricBounds();

    this.chart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.labels().map(l => new Date(l - 86400000).toLocaleDateString('en-US', { day: 'numeric', month: 'short' })),
        datasets: [
          {
            label: 'Flow',
            data: this.netFlow(),
            borderColor: '#c08210ff',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 3,
            pointRadius: 0,
            pointHoverRadius: 6,
            tension: 0.4,
            fill: {
              target: 'origin',
              below: 'rgba(255, 0, 0, 0.1)',
              above: 'rgba(0, 255, 0, 0.1)'
            }
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
                return `📅 ${date.toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' })}`;
              },
              label: (ctx) => { // Les données de l'info-bulle selon le curseur (ctx du jour)
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
              color: (context) => context.tick.value === 0 ? 'rgba(75, 37, 2, 0.88)' : 'transparent',
              lineWidth: (context) => context.tick.value === 0 ? 4 : 0
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
