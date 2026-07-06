import { Component, computed, input } from '@angular/core';
import type { PriceSource, MarketStatus } from '../../../core/models';
import { formatTime } from '../../../core/services/format-dates';

/** Fallback when registry sync interval is unknown (2 × 15 min). */
const DEFAULT_STALE_THRESHOLD_MS = 30 * 60 * 1000;

@Component({
  selector: 'app-price-freshness-badge',
  imports: [],
  templateUrl: './price-freshness-badge.html',
})
export class PriceFreshnessBadge {
  marketStatus = input<MarketStatus | null>(null);
  priceSource = input<PriceSource | null>(null);
  lastRefresh = input<number | null>(null);
  /** Registry sync.interval-minutes; stale when age exceeds 2× this value (ADR-14). */
  syncIntervalMinutes = input<number | null>(null);

  private staleThresholdMs = computed(() => {
    const interval = this.syncIntervalMinutes();
    if (interval != null && interval > 0) {
      return interval * 2 * 60 * 1000;
    }
    return DEFAULT_STALE_THRESHOLD_MS;
  });

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
    const isStaleTime = refresh === null || Date.now() - refresh > threshold;

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
