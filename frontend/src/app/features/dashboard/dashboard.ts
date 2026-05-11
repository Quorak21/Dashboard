import { Component, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs/operators';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  private http = inject(HttpClient);

  hypePrice = toSignal(
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/hype`).pipe(
      map(data => data?.currentPrice ?? null)
    ),
    { initialValue: null }
  );
}
