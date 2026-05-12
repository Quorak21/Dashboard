import { Component, inject, computed } from '@angular/core';
import { signal } from '@angular/core';
import { Subscription, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { formatNumber } from '../../crypto/services/format-number';
import { formatTime } from '../../crypto/services/format-dates';
import { environment } from '../../../../environments/environment';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { CurrencyService } from '../../../core/services/currency.service';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';

@Component({
  selector: 'app-inveb',
  imports: [AssetMainCard, PriceChart],
  templateUrl: './inveb.html',
  styleUrl: './inveb.css',
})
export class Inveb {

  public formatNumber = formatNumber;
  public formatTime = formatTime;

  private http = inject(HttpClient);
  public currencyService = inject(CurrencyService);

  private timerSub: Subscription = timer(0, 60000).subscribe(() => this.refresh());

  usdPrice = signal<number>(0);
  chfPrice = signal<number>(0);

  // Computed price based on toggle
  currentPrice = computed(() => this.currencyService.wantChf() ? this.chfPrice() : this.usdPrice());
  currencySymbol = computed(() => this.currencyService.wantChf() ? 'CHF' : '$');

  priceChangePercentage24h = signal<number>(0);
  totalVolume = signal<number>(0);
  marketCap = signal<number>(0);
  symbol = signal<string>('INVE-B');
  lastRefresh = signal<number>(0);
  historyPrices = signal<number[]>([]);
  historyDays = signal<number[]>([]);

  refresh() {
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/inveb`).subscribe((data: any) => {
      this.usdPrice.set(data.currentPrice);
      this.chfPrice.set(data.currentPriceChf);
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
