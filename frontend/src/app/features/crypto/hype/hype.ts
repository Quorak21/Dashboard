import { Component, inject, OnDestroy } from '@angular/core';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';
import { DailyChart } from '../../../shared/components/daily-chart/daily-chart';
import { HttpClient } from '@angular/common/http';
import { formatNumber } from '../services/format-number';
import { formatTime } from '../services/format-dates';
import { signal } from '@angular/core';
import { timer, Subscription } from 'rxjs';

@Component({
  selector: 'app-hype',
  standalone: true,
  imports: [AssetMainCard, PriceChart, DailyChart],
  templateUrl: './hype.html',
  styleUrl: './hype.css',
})
export class Hype implements OnDestroy {

  public formatNumber = formatNumber;
  public formatTime = formatTime;

  private http = inject(HttpClient);
  private timerSub: Subscription = timer(0, 300000).subscribe(() => this.refresh());
  currentPrice = signal<number>(0);
  priceChangePercentage24h = signal<number>(0);
  totalVolume = signal<number>(0);
  marketCap = signal<number>(0);
  symbol = signal<string>('HYPE');
  lastRefresh = signal<number>(0);

  historyPrices = signal<number[]>([]);
  historyDays = signal<number[]>([]);
  livePrices = signal<number[]>([]);
  liveDays = signal<number[]>([]);

  refresh() {
    this.http.get<any>('http://localhost:8080/api/dashboard/hype').subscribe((data: any) => {
      this.currentPrice.set(data.currentPrice);
      this.priceChangePercentage24h.set(data.priceChangePercentage24h);
      this.totalVolume.set(data.totalVolume);
      this.marketCap.set(data.marketCap);
      this.symbol.set(data.symbol);
      this.lastRefresh.set(data.lastRefresh);
      this.historyPrices.set(data.historyPrices);
      this.historyDays.set(data.historyDays);
      this.livePrices.set(data.livePrices);
      this.liveDays.set(data.liveDays);
    });



  }

  ngOnDestroy() {
    this.timerSub.unsubscribe();
  }

}
