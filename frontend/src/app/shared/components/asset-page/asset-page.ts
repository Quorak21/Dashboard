import { Component, inject, computed, DestroyRef, signal, input } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import type { AssetDto } from '../../../core/models';
import { AssetMainCard } from '../asset-main-card/asset-main-card';
import { PriceChart } from '../price-chart/price-chart';
import { DailyChart } from '../daily-chart/daily-chart';
import { DividendCard } from '../dividend-card/dividend-card';
import { FundamentalsCard } from '../fundamentals-card/fundamentals-card';
import { EtfMetricsCard } from '../etf-metrics-card/etf-metrics-card';
import { EtfSectorChart } from '../etf-sector-chart/etf-sector-chart';
import { PriceFreshnessBadge } from '../price-freshness-badge/price-freshness-badge';
import { Subject, switchMap, tap, takeUntil, of, catchError } from 'rxjs';
import { ASSET_PAGE_LABELS } from './asset-page.labels';

@Component({
  selector: 'app-asset-page',
  imports: [
    AssetMainCard,
    PriceChart,
    DailyChart,
    DividendCard,
    FundamentalsCard,
    EtfMetricsCard,
    EtfSectorChart,
    PriceFreshnessBadge,
  ],
  templateUrl: './asset-page.html',
})
export class AssetPage {
  assetId = input.required<string>();

  readonly labels = ASSET_PAGE_LABELS;

  data = signal<AssetDto | null>(null);
  loading = signal<boolean>(true);

  private api = inject(DashboardApiService);
  private destroyRef = inject(DestroyRef);
  private destroy$ = new Subject<void>();

  assetName = computed(() => this.data()?.displayName ?? '');
  symbol = computed(() => this.data()?.symbol ?? '');
  currentPrice = computed(() => this.data()?.currentPrice ?? null);
  currency = computed(() => this.data()?.currency ?? 'SEK');
  priceChangePercentage24h = computed(() => this.data()?.priceChangePercentage24h ?? null);
  totalVolume = computed(() => this.data()?.totalVolume ?? null);
  marketCap = computed(() => this.data()?.marketCap ?? null);
  lastRefresh = computed(() => this.data()?.lastRefresh ?? null);
  priceSource = computed(() => this.data()?.priceSource ?? null);
  marketStatus = computed(() => this.data()?.marketStatus ?? null);
  syncIntervalMinutes = computed(() => this.data()?.syncIntervalMinutes ?? null);

  historyPrices = computed(() => this.data()?.historyPrices ?? []);
  historyDays = computed(() => this.data()?.historyDays ?? []);
  livePrices = computed(() => this.data()?.livePrices ?? []);
  liveDays = computed(() => this.data()?.liveDays ?? []);

  dividends = computed(() => this.data()?.dividends ?? null);
  fundamentals = computed(() => this.data()?.fundamentals ?? null);
  isEtf = computed(() => this.data()?.type === 'ETF');
  sectorWeights = computed(() => this.data()?.fundamentals?.sectorWeights ?? []);

  hasLiveData = computed(() => this.currentPrice() !== null);
  isMarketClosed = computed(() => this.marketStatus() === 'CLOSED');

  constructor() {
    toObservable(this.assetId)
      .pipe(
        tap(() => this.loading.set(true)),
        switchMap((id) =>
          this.api.getAsset(id).pipe(
            catchError((err) => {
              console.error('Error fetching asset data', err);
              this.loading.set(false);
              return of(null);
            })
          )
        ),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((dto) => {
        this.data.set(dto);
        this.loading.set(false);
      });

    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => {
      clearInterval(intervalID);
      this.destroy$.next();
      this.destroy$.complete();
    });
  }

  refresh() {
    const id = this.assetId();
    if (!id) return;
    this.api
      .getAsset(id)
      .pipe(
        takeUntil(this.destroy$),
        catchError(() => of(null))
      )
      .subscribe((dto) => {
        if (dto && id === this.assetId()) {
          this.data.set(dto);
        }
      });
  }
}
