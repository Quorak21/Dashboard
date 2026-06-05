import { Component, computed, input } from '@angular/core';
import { formatNumber } from '../../../../core/services/format-number';

const INVEB_DIVIDEND_HISTORY_PLACEHOLDER = [
  { year: 2016, amount: '2.50 SEK' },
  { year: 2017, amount: '2.75 SEK' },
  { year: 2018, amount: '3.00 SEK' },
  { year: 2019, amount: '3.25 SEK' },
  { year: 2020, amount: '2.25 SEK' },
  { year: 2021, amount: '3.50 SEK' },
  { year: 2022, amount: '4.00 SEK' },
  { year: 2023, amount: '4.40 SEK' },
  { year: 2024, amount: '4.80 SEK' },
  { year: 2025, amount: '5.20 SEK' },
  { year: 2026, amount: '5.60 SEK' },
] as const;

@Component({
  selector: 'app-inveb-dividend-card',
  imports: [],
  templateUrl: './inveb-dividend-card.html',
  styleUrl: './inveb-dividend-card.css',
})
export class InvebDividendCard {
  private readonly projectionYearValue = 2027;
  private readonly projectionDividendSekValue = '6.00';
  private readonly avgGrowth10YValue = '8.40 %';

  currentPrice = input<number | null>(null);
  hasData = input<boolean>(false);

  historyRows = computed(() => {
    const rows = INVEB_DIVIDEND_HISTORY_PLACEHOLDER.slice(-5);
    if (!this.hasData()) {
      return rows.map((row) => ({ year: row.year, amount: '-' }));
    }
    return rows.map((row) => ({ year: row.year, amount: row.amount }));
  });

  avgGrowth10Y = computed(() => (this.hasData() ? this.avgGrowth10YValue : '-'));

  projectionYear = computed(() => (this.hasData() ? this.projectionYearValue : '-'));

  projectionDividendSek = computed(() =>
    this.hasData() ? `${this.projectionDividendSekValue} SEK` : '-',
  );

  estimatedYield = computed(() => {
    if (!this.hasData()) {
      return null;
    }
    const price = this.currentPrice();
    if (price == null || price <= 0) {
      return null;
    }
    return (Number(this.projectionDividendSekValue) / price) * 100;
  });

  estimatedYieldLabel = computed(() => {
    const yieldPct = this.estimatedYield();
    if (yieldPct === null) {
      return null;
    }
    return `${formatNumber(yieldPct)}%`;
  });
}
