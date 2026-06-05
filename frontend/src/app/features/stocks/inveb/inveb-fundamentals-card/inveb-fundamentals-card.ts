import { Component, computed, input } from '@angular/core';

export interface InvebFundamentalMetric {
  label: string;
  value: string;
}

export interface InvebTopHolding {
  name: string;
  weight: string;
}

/** Hardcoded Q1 2026 report — updated manually ~once per year. */
export const INVEB_FUNDAMENTALS_METRICS: InvebFundamentalMetric[] = [
  { label: 'Trailing P/E', value: '6.11' },
  { label: 'Debt Leverage', value: '1.2%' },
  { label: 'Management Cost', value: '0.09%' },
  { label: '5Y NAV CAGR', value: '14.5%' },
  { label: '5Y Total Return', value: '112%' },
  { label: 'Dry Powder', value: '~18.2B SEK' },
  { label: 'Cash Inflow', value: '~11.5B SEK' },
];

export const INVEB_TOP_HOLDINGS: InvebTopHolding[] = [
  { name: 'ABB', weight: '16.5%' },
  { name: 'Atlas Copco', weight: '15%' },
  { name: 'SEB', weight: '11.5%' },
  { name: 'AstraZeneca', weight: '8%' },
  { name: 'Mölnlycke', weight: '7.5%' },
  { name: 'Epiroc', weight: '5.5%' },
  { name: 'EQT AB', weight: '5%' },
  { name: 'Ericsson', weight: '4%' },
  { name: 'Nasdaq', weight: '3.5%' },
  { name: 'Saab', weight: '3%' },
];

@Component({
  selector: 'app-inveb-fundamentals-card',
  imports: [],
  templateUrl: './inveb-fundamentals-card.html',
  styleUrl: './inveb-fundamentals-card.css',
})
export class InvebFundamentalsCard {
  hasData = input<boolean>(false);

  metrics = computed(() =>
    this.hasData()
      ? INVEB_FUNDAMENTALS_METRICS
      : INVEB_FUNDAMENTALS_METRICS.map((metric) => ({ label: metric.label, value: '-' })),
  );

  topHoldings = computed(() =>
    this.hasData()
      ? INVEB_TOP_HOLDINGS
      : INVEB_TOP_HOLDINGS.map((holding) => ({ name: holding.name, weight: '-' })),
  );

  sourceLabel = computed(() => (this.hasData() ? 'Source: Q1 2026 report' : '-'));
}
