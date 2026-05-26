import { Component, inject, computed } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';
import { DashboardApiService } from '../../core/services/dashboard-api.service';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {

  private api = inject(DashboardApiService);

  // TODO: N'attrape le prix qu'une seule fois, il n'y a pas de refresh actuellement. A modifier
  private rawHype = toSignal(this.api.getData('hype'), { initialValue: null });

  private rawInveb = toSignal( this.api.getData('inveb'), { initialValue: null });

  hypePrice = computed(() => this.rawHype()?.currentPrice ?? null);
  invebPrice = computed(() => this.rawInveb()?.currentPrice ?? null);

  hypeChange = computed(() => this.rawHype()?.priceChangePercentage24h ?? null);
  invebChange = computed(() => this.rawInveb()?.priceChangePercentage24h ?? null);

}
