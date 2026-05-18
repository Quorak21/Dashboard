import { Component, inject, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';
import { environment } from '../../../environments/environment';
import { catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private http = inject(HttpClient);

  private rawHype = toSignal(
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/hype`).pipe(catchError(() => of(null))),
    { initialValue: null }
  );

  private rawInveb = toSignal(
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/inveb`).pipe(catchError(() => of(null))),
    { initialValue: null }
  );

  hypePrice = computed(() => this.rawHype()?.currentPrice ?? 0);
  invebPrice = computed(() => this.rawInveb()?.currentPrice ?? 0);

  hypeChange = computed(() => this.rawHype()?.priceChangePercentage24h ?? 0);
  invebChange = computed(() => this.rawInveb()?.priceChangePercentage24h ?? 0);

}
