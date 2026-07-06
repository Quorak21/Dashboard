import { Component, computed, input } from '@angular/core';
import { formatNumber } from '../../../core/services/format-number';
import type { FundamentalsBlock, HoldingEntry, SectorWeight } from '../../../core/models';

const KEY_LABELS: Record<string, string> = {
  'trailing-pe': 'Trailing P/E',
  'debt-leverage': 'Debt Leverage',
  'management-cost': 'Management Cost',
  'five-y-nav-cagr': '5Y NAV CAGR',
  'five-y-total-return': '5Y Total Return',
  'dry-powder': 'Dry Powder',
  'cash-inflow': 'Cash Inflow',
  'five-y-avg-discount': '5Y Avg Discount',
  'gearing-leverage': 'Gearing / Leverage',
  'ongoing-charges-ter': 'Ongoing Charges (TER)',
  'ten-y-avg-div-growth': '10Y Avg Div Growth',
  'top-3-assets-weight': 'Top 3 Assets Weight',
  'copper-base-metals': 'Copper & Base Metals',
  'gold-precious-metals': 'Gold & Precious Metals',
  'mining-royalties-bonds': 'Mining Royalties & Bonds',
  'forward-pe': 'Forward P/E',
  'net-debt-ebitda': 'Net Debt / EBITDAre',
  'affo-payout-ratio': 'AFFO Payout Ratio',
  'portfolio-occupancy': 'Portfolio Occupancy',
  'dividend-payout-ratio': 'Dividend Payout Ratio',
};

const METRIC_ORDER = [
  'trailing-pe',
  'forward-pe',
  'net-debt-ebitda',
  'debt-leverage',
  'affo-payout-ratio',
  'dividend-payout-ratio',
  'portfolio-occupancy',
  'management-cost',
  'five-y-nav-cagr',
  'five-y-total-return',
  'dry-powder',
  'cash-inflow',
  'five-y-avg-discount',
  'gearing-leverage',
  'ongoing-charges-ter',
  'ten-y-avg-div-growth',
  'top-3-assets-weight',
  'copper-base-metals',
  'gold-precious-metals',
  'mining-royalties-bonds',
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

  sectorWeights = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.sectorWeights) {
      return [];
    }
    return this.mapSectorWeights(fund.sectorWeights);
  });

  retailIndustryWeights = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.retailIndustryWeights) {
      return [];
    }
    return this.mapSectorWeights(fund.retailIndustryWeights);
  });

  private mapSectorWeights(weights: SectorWeight[]) {
    return [...weights]
      .filter((sw): sw is SectorWeight => sw != null && typeof sw.sector === 'string')
      .sort((a, b) => (b.weightPercent ?? 0) - (a.weightPercent ?? 0))
      .map((sw: SectorWeight) => {
        const weightStr =
          sw.weightPercent != null && !isNaN(sw.weightPercent)
            ? `${formatNumber(sw.weightPercent)}%`
            : '-';
        return {
          name: sw.sector,
          weight: weightStr,
        };
      });
  }

  allocationTitle = computed(() =>
    this.topHoldings().length > 0 ? 'Holdings' : 'Portfolio Mix (ABR)',
  );

  allocationRows = computed(() => {
    const holdings = this.topHoldings();
    if (holdings.length > 0) {
      return holdings;
    }
    return this.sectorWeights();
  });

  hasAllocation = computed(() => this.allocationRows().length > 0);

  hasRetailIndustryDetail = computed(() => this.retailIndustryWeights().length > 0);

  sourceLabel = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.source) {
      return '-';
    }
    return fund.source;
  });
}
