import { Component, inject, computed, DestroyRef } from '@angular/core';
import { signal } from '@angular/core';
import type { InveBDto } from '../../../core/models';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';
import { DailyChart } from '../../../shared/components/daily-chart/daily-chart';
import { InvebDividendCard } from './inveb-dividend-card/inveb-dividend-card';
import { InvebFundamentalsCard } from './inveb-fundamentals-card/inveb-fundamentals-card';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-inveb',
  imports: [AssetMainCard, PriceChart, DailyChart, InvebDividendCard, InvebFundamentalsCard],
  templateUrl: './inveb.html',
  styleUrl: './inveb.css',
})
export class Inveb {
  data = signal<InveBDto | null>(null);

  private api = inject(DashboardApiService);
  private destroyRef = inject(DestroyRef);

  // On récupère les data dont on a besoin de data, mis à jour au refresh tous les 3minutes
  currentPrice = computed(() => this.data()?.currentPrice ?? null);
  currencySymbol = computed(() => 'SEK');
  priceChangePercentage24h = computed(() => this.data()?.priceChangePercentage24h ?? null);
  totalVolume = computed(() => this.data()?.totalVolume ?? null);
  marketCap = computed(() => this.data()?.marketCap ?? null);
  symbol = computed(() => this.data()?.symbol ?? 'INVE-B');
  lastRefresh = computed(() => this.data()?.lastRefresh ?? null);
  historyPrices = computed(() => this.data()?.historyPrices ?? []);
  historyDays = computed(() => this.data()?.historyDays ?? []);
  livePrices = computed(() => this.data()?.livePrices ?? []);
  liveDays = computed(() => this.data()?.liveDays ?? []);

  hasLiveData = computed(() => this.currentPrice() !== null);

  isMarketClosed = computed(() => {
    const refreshTime = this.lastRefresh();
    if (!refreshTime) return false;

    try {
      const date = new Date(refreshTime);
      const stockholmStr = date.toLocaleString('en-US', { timeZone: 'Europe/Stockholm' });
      const stockholmDate = new Date(stockholmStr);
      const day = stockholmDate.getDay();
      const hour = stockholmDate.getHours();
      const minute = stockholmDate.getMinutes();

      const isWeekend = day === 0 || day === 6;
      const isBeforeOpen = hour < 9;
      const isAfterClose = hour > 17 || (hour === 17 && minute > 30);

      return isWeekend || isBeforeOpen || isAfterClose;
    } catch (e) {
      return false;
    }
  });

  // Constructeur — initialisation du refresh et du timer
  constructor() {
    this.refresh();
    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => clearInterval(intervalID));
  }

  // TODO: Message d'erreur si on reçoit plus rien, la dernière donnée valide reste mais plus de mise à jour. Appartition tag rouge "erreur donnée plus a jour"
  refresh() {
    this.api
      .getData('inveb')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data: InveBDto | null) => this.data.set(data));
  }
}
