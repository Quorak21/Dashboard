import { Component, computed, input } from '@angular/core';
import type { PriceSource, MarketStatus } from '../../../core/models';
import { formatTime } from '../../../core/services/format-dates';

@Component({
  selector: 'app-price-freshness-badge',
  imports: [],
  templateUrl: './price-freshness-badge.html',
})
export class PriceFreshnessBadge {
  marketStatus = input<MarketStatus | null>(null);
  priceSource = input<PriceSource | null>(null);
  lastRefresh = input<number | null>(null);

  // default threshold: 30 minutes
  staleThresholdMs = input<number>(30 * 60 * 1000);

  freshness = computed(() => {
    const status = this.marketStatus();
    const source = this.priceSource();
    const refresh = this.lastRefresh();
    const threshold = this.staleThresholdMs();

    if (status === 'CLOSED') {
      return {
        type: 'closed',
        label: 'Marché Fermé',
        badgeClass: 'bg-dark-700/50 text-copper-300/60 border border-copper-500/10',
        bulletClass: 'bg-copper-500/40',
      };
    }

    const isStaleSource = source === 'CACHE';
    const isStaleTime = refresh === null || (Date.now() - refresh > threshold);

    if (isStaleSource || isStaleTime) {
      return {
        type: 'stale',
        label: isStaleSource ? 'Différé' : 'Donnée Obsolète',
        badgeClass: 'bg-red-950/40 text-red-400 border border-red-500/20',
        bulletClass: 'bg-red-500 animate-pulse',
      };
    }

    return {
      type: 'live',
      label: 'En Direct',
      badgeClass: 'bg-green-950/40 text-green-400 border border-green-500/20',
      bulletClass: 'bg-green-500 animate-pulse',
    };
  });

  formattedTime = computed(() => {
    const refresh = this.lastRefresh();
    if (refresh === null) return '--:--';
    return formatTime(refresh);
  });
}
