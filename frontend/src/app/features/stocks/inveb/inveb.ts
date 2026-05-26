import { Component, inject, computed, DestroyRef } from '@angular/core';
import { signal } from '@angular/core';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import { formatNumber } from '../../../core/services/format-number';
import { formatTime } from '../../../core/services/format-dates';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-inveb',
  imports: [AssetMainCard, PriceChart],
  templateUrl: './inveb.html',
  styleUrl: './inveb.css',
})
export class Inveb {

  // TODO: Toujours ce problème de any
  data = signal<any>(null);

  public formatNumber = formatNumber;
  public formatTime = formatTime;
  private api = inject(DashboardApiService);
  private destroyRef = inject(DestroyRef);

  // On récupère les data dont on a besoin de data, mis à jour au refresh tous les 3minutes 
  currentPrice = computed(() => this.data()?.currentPrice ?? 0);
  currencySymbol = computed(() => 'SEK');
  priceChangePercentage24h = computed(() => this.data()?.priceChangePercentage24h ?? 0);
  totalVolume = computed(() => this.data()?.totalVolume ?? 0);
  marketCap = computed(() => this.data()?.marketCap ?? 0);
  symbol = computed(() => this.data()?.symbol ?? 'INVE-B');
  lastRefresh = computed(() => this.data()?.lastRefresh ?? 0);
  historyPrices = computed(() => this.data()?.historyPrices ?? []);
  historyDays = computed(() => this.data()?.historyDays ?? []);

  // Constructeur — initialisation du refresh et du timer
  constructor() {
    this.refresh();
    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => clearInterval(intervalID));
  }

  // TODO: Message d'erreur si on reçoit plus rien, la dernière donnée valide reste mais plus de mise à jour. Appartition tag rouge "erreur donnée plus a jour"
  refresh() {
    this.api.getData('inveb').pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => this.data.set(data));
  }
}
