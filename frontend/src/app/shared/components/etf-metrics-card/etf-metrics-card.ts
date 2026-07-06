import { Component, computed, input } from '@angular/core';
import { formatNumber } from '../../../core/services/format-number';
import type { FundamentalsBlock, HoldingEntry } from '../../../core/models';

@Component({
  selector: 'app-etf-metrics-card',
  imports: [],
  templateUrl: './etf-metrics-card.html',
  styleUrl: './etf-metrics-card.css',
})
export class EtfMetricsCard {
  fundamentals = input<FundamentalsBlock | null>(null);

  hasData = computed(() => this.fundamentals() !== null);

  ter = computed(() => {
    const fund = this.fundamentals();
    if (!fund || !fund.metrics) return '-';
    return String(fund.metrics['management-fee'] ?? '-');
  });

  aum = computed(() => {
    const fund = this.fundamentals();
    if (!fund || !fund.metrics) return '-';
    return String(fund.metrics['total-assets'] ?? '-');
  });

  navPremium = computed(() => {
    const fund = this.fundamentals();
    if (!fund || !fund.metrics) return '-';
    return String(fund.metrics['nav-discount-premium'] ?? '-');
  });

  topHoldings = computed(() => {
    const fund = this.fundamentals();
    if (!this.hasData() || !fund || !fund.topHoldings) {
      return [];
    }
    return fund.topHoldings
      .filter((h): h is HoldingEntry => h != null)
      .map((h: HoldingEntry) => {
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
