import { Component, inject, computed, OnDestroy } from '@angular/core';
import { signal } from '@angular/core';
import { Subscription, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { formatNumber } from '../../../core/services/format-number';
import { formatTime } from '../../../core/services/format-dates';
import { environment } from '../../../../environments/environment';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';

@Component({
  selector: 'app-inveb',
  imports: [AssetMainCard, PriceChart],
  templateUrl: './inveb.html',
  styleUrl: './inveb.css',
})
export class Inveb implements OnDestroy {

  public formatNumber = formatNumber;
  public formatTime = formatTime;

  private http = inject(HttpClient);

  private timerSub: Subscription = timer(0, 60000).subscribe(() => this.refresh());

  currentPrice = signal<number>(0);
  currencySymbol = computed(() => 'SEK');

  priceChangePercentage24h = signal<number>(0);
  totalVolume = signal<number>(0);
  marketCap = signal<number>(0);
  symbol = signal<string>('INVE-B');
  lastRefresh = signal<number>(0);
  historyPrices = signal<number[]>([]);
  historyDays = signal<number[]>([]);

  refresh() {
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/inveb`).subscribe((data: any) => {
      this.currentPrice.set(data.currentPrice);
      this.priceChangePercentage24h.set(data.priceChangePercentage24h);
      this.totalVolume.set(data.totalVolume);
      this.marketCap.set(data.marketCap);
      this.symbol.set(data.symbol);
      this.lastRefresh.set(data.lastRefresh);
      this.historyPrices.set(data.historyPrices);
      this.historyDays.set(data.historyDays);
    });
  }

  ngOnDestroy() {
    this.timerSub.unsubscribe();
  }
}
