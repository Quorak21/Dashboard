import { Component, computed, input } from '@angular/core';
import { formatNumber, formatCurrency } from '../../../core/services/format-number';
import type { DividendsBlock, DividendHistoryEntry } from '../../../core/models';

const HISTORY_YEARS = 10;

function computeCagrPercent(history: DividendHistoryEntry[]): number | null {
  const sorted = history
    .filter(
      (entry) =>
        entry &&
        typeof entry.year === 'number' &&
        !isNaN(entry.year) &&
        entry.amount !== null &&
        entry.amount !== undefined &&
        !isNaN(entry.amount) &&
        entry.amount > 0
    )
    .sort((a, b) => a.year - b.year)
    .slice(-HISTORY_YEARS);

  if (sorted.length < 2) {
    return null;
  }

  const first = sorted[0].amount as number;
  const last = sorted[sorted.length - 1].amount as number;
  const periods = sorted.length - 1;

  return (Math.pow(last / first, 1 / periods) - 1) * 100;
}

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
      return Array.from({ length: HISTORY_YEARS }, (_, i) => ({
        year: currentYear - i,
        amount: '-',
      }));
    }
    const validEntries = div.history.filter(
      (entry) => entry && typeof entry.year === 'number' && !isNaN(entry.year)
    );
    const sorted = [...validEntries].sort((a, b) => b.year - a.year);
    const last10 = sorted.slice(0, HISTORY_YEARS);
    return last10.map((entry: DividendHistoryEntry) => {
      const formattedAmount =
        entry.amount !== null && entry.amount !== undefined && !isNaN(entry.amount)
          ? formatCurrency(entry.amount, entry.currency || this.currency() || '')
          : '-';
      return {
        year: entry.year,
        amount: formattedAmount,
      };
    });
  });

  avgGrowth10Y = computed(() => {
    const div = this.dividends();
    if (!this.hasData() || !div) {
      return '-';
    }

    const fromHistory =
      div.history && div.history.length > 0 ? computeCagrPercent(div.history) : null;
    const configured = div.avgDividendGrowth10Y;
    const growth =
      fromHistory !== null && !isNaN(fromHistory)
        ? fromHistory
        : configured !== null && configured !== undefined && !isNaN(configured)
          ? configured
          : null;

    if (growth === null) {
      return '-';
    }
    return `${formatNumber(growth)} %`;
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
    return formatCurrency(div.forwardDividend, currencyStr);
  });

  estimatedYieldLabel = computed(() => {
    const div = this.dividends();
    if (!this.hasData() || !div || div.estimatedYield === null || div.estimatedYield === undefined || isNaN(div.estimatedYield)) {
      return null;
    }
    return `${formatNumber(div.estimatedYield)}%`;
  });
}
