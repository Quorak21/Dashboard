import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CurrencyService {
  private http = inject(HttpClient);
  
  selectedCurrency = signal<'USD' | 'CHF' | 'EUR'>('USD');
  usdChf = signal<number>(1);
  usdEur = signal<number>(1);
  sekChf = signal<number>(1);
  sekUsd = signal<number>(1);

  constructor() {
    this.refreshRates();
  }

  setCurrency(currency: 'USD' | 'CHF' | 'EUR') {
    this.selectedCurrency.set(currency);
  }

  refreshRates() {
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/rates`).subscribe(rates => {
      this.usdChf.set(rates.USD_CHF || 1);
      this.usdEur.set(rates.USD_EUR || 1);
      this.sekChf.set(rates.SEK_CHF || 1);
      this.sekUsd.set(rates.SEK_USD || 1);
    });
  }
}
