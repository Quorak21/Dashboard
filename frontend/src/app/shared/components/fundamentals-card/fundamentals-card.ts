import { Component, computed, input } from '@angular/core';
import { formatNumber } from '../../../core/services/format-number';
import type { FundamentalsBlock, HoldingEntry } from '../../../core/models';

const KEY_LABELS: Record<string, string> = {
  'trailing-pe': 'Trailing P/E',
  'debt-leverage': 'Debt Leverage',
  'management-cost': 'Management Cost',
  'five-y-nav-cagr': '5Y NAV CAGR',
  'five-y-total-return': '5Y Total Return',
  'dry-powder': 'Dry Powder',
  'cash-inflow': 'Cash Inflow',
};

const METRIC_ORDER = [
  'trailing-pe',
  'debt-leverage',
  'management-cost',
  'five-y-nav-cagr',
  'five-y-total-return',
  'dry-powder',
  'cash-inflow',
];

function formatKey(key: string): string {
  if (KEY_LABELS[key]) {
    return KEY_LABELS[key];
  }
  return key
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

@Component({
  selector: 'app-fundamentals-card',
  imports: [],
  templateUrl: './fundamentals-card.html',
  styleUrl: './fundamentals-card.css',
})
export class FundamentalsCard {
  fundamentals = input<FundamentalsBlock | null>(null);

  hasData = computed(() => this.fundamentals() !== null);

  metrics = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.metrics) {
      return [];
    }
    const entries = Object.entries(fund.metrics);
    const sortedEntries = entries.sort((a, b) => {
      const idxA = METRIC_ORDER.indexOf(a[0]);
      const idxB = METRIC_ORDER.indexOf(b[0]);
      if (idxA !== -1 && idxB !== -1) return idxA - idxB;
      if (idxA !== -1) return -1;
      if (idxB !== -1) return 1;
      return a[0].localeCompare(b[0]);
    });

    return sortedEntries.map(([key, val]) => {
      let formattedVal = '-';
      if (val !== null && val !== undefined) {
        if (typeof val === 'number') {
          if (!isNaN(val)) {
            formattedVal = formatNumber(val);
          }
        } else {
          formattedVal = String(val);
        }
      }
      return {
        label: formatKey(key),
        value: formattedVal,
      };
    });
  });

  topHoldings = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.topHoldings) {
      return [];
    }
    return fund.topHoldings.map((h: HoldingEntry) => {
      const weightStr = h.weightPercent != null && !isNaN(h.weightPercent)
        ? `${formatNumber(h.weightPercent)}%`
        : '-';
      return {
        name: h.name,
        weight: weightStr,
      };
    });
  });

  sourceLabel = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.source) {
      return '-';
    }
    return fund.source;
  });
}
