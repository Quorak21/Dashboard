import { Component, inject, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';
import { environment } from '../../../environments/environment';
import { CurrencyService } from '../../core/services/currency.service';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private http = inject(HttpClient);
  public currencyService = inject(CurrencyService);

  private rawHype = toSignal(
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/hype`),
    { initialValue: null }
  );

  private rawInveb = toSignal(
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/inveb`),
    { initialValue: null }
  );

  private rawHypePrice = computed(() => this.rawHype()?.currentPrice ?? 0);
  private rawInvebPrice = computed(() => this.rawInveb()?.currentPrice ?? 0);
  
  hypeChange = computed(() => this.rawHype()?.priceChangePercentage24h ?? 0);
  invebChange = computed(() => this.rawInveb()?.priceChangePercentage24h ?? 0);

  hypePrice = computed(() => {
    const price = this.rawHypePrice();
    const cur = this.currencyService.selectedCurrency();
    if (cur === 'CHF') return price * this.currencyService.usdChf();
    if (cur === 'EUR') return price * this.currencyService.usdEur();
    return price;
  });

  invebPrice = computed(() => {
    const price = this.rawInvebPrice();
    const cur = this.currencyService.selectedCurrency();
    if (cur === 'CHF') return price * this.currencyService.usdChf();
    if (cur === 'EUR') return price * this.currencyService.usdEur();
    return price;
  });

  currencySymbol = computed(() => {
    const cur = this.currencyService.selectedCurrency();
    if (cur === 'CHF') return 'CHF';
    if (cur === 'EUR') return '€';
    return '$';
  });
}
