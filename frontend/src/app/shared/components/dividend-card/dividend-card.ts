import { Component, computed, input } from '@angular/core';
import { formatNumber } from '../../../core/services/format-number';
import type { DividendsBlock, DividendHistoryEntry } from '../../../core/models';

@Component({
  selector: 'app-dividend-card',
  imports: [],
  templateUrl: './dividend-card.html',
  styleUrl: './dividend-card.css',
})
export class DividendCard {
  dividends = input<DividendsBlock | null>(null);
  currency = input<string | null>(null);
  currentYearInput = input<number | null>(null);

  hasData = computed(() => this.dividends() !== null);
  computedCurrentYear = computed(() => this.currentYearInput() || new Date().getFullYear());

  historyRows = computed(() => {
    const div = this.dividends();
    if (!this.hasData() || !div || !div.history) {
      const currentYear = this.computedCurrentYear();
      return Array.from({ length: 5 }, (_, i) => ({
        year: currentYear - 5 + i,
        amount: '-',
      }));
    }
    const validEntries = div.history.filter(
      (entry) => entry && typeof entry.year === 'number' && !isNaN(entry.year)
    );
    const sorted = [...validEntries].sort((a, b) => a.year - b.year);
    const last5 = sorted.slice(-5);
    return last5.map((entry: DividendHistoryEntry) => {
      const formattedAmount =
        entry.amount !== null && entry.amount !== undefined && !isNaN(entry.amount)
          ? formatNumber(entry.amount)
          : '-';
      return {
        year: entry.year,
        amount: `${formattedAmount} ${entry.currency || this.currency() || ''}`.trim(),
      };
    });
  });

  avgGrowth10Y = computed(() => {
    const div = this.dividends();
    if (!this.hasData() || !div || div.avgDividendGrowth10Y === null || div.avgDividendGrowth10Y === undefined || isNaN(div.avgDividendGrowth10Y)) {
      return '-';
    }
    return `${formatNumber(div.avgDividendGrowth10Y)} %`;
  });

  projectionYear = computed(() => {
    if (!this.hasData()) {
      return '-';
    }
    const div = this.dividends();
    if (!div || !div.history || div.history.length === 0) {
      return String(this.computedCurrentYear() + 1);
    }
    const validYears = div.history
      .map((h: DividendHistoryEntry) => h.year)
      .filter((y) => typeof y === 'number' && !isNaN(y));
    const maxYear = validYears.length > 0 ? Math.max(...validYears) : this.computedCurrentYear();
    return String(maxYear + 1);
  });

  projectionDividend = computed(() => {
    const div = this.dividends();
    if (!this.hasData() || !div || div.forwardDividend === null || div.forwardDividend === undefined || isNaN(div.forwardDividend)) {
      return '-';
    }
    const currencyStr = div.forwardDividendCurrency || this.currency() || '';
    return `${formatNumber(div.forwardDividend)} ${currencyStr}`.trim();
  });

  estimatedYieldLabel = computed(() => {
    const div = this.dividends();
    if (!this.hasData() || !div || div.estimatedYield === null || div.estimatedYield === undefined || isNaN(div.estimatedYield)) {
      return null;
    }
    return `${formatNumber(div.estimatedYield)}%`;
  });
}
