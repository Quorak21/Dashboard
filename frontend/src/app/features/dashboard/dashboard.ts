import { Component, inject, computed, signal, DestroyRef } from '@angular/core';
import { Subscription } from 'rxjs';
import type { HypeDto, AssetDto } from '../../core/models';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { LucideAngularModule, Zap, TrendingUp, Boxes } from 'lucide-angular';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard, LucideAngularModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly Zap = Zap;
  readonly TrendingUp = TrendingUp;
  readonly Boxes = Boxes;

  private api = inject(DashboardApiService);
  private destroyRef = inject(DestroyRef);

  private readonly assetIds = ['inveb', 'brwm', 'o'];

  private hypeData = signal<HypeDto | null>(null);
  
  // Dynamic assets map
  assets = signal<Record<string, AssetDto | null>>({});

  private hypeSub?: Subscription;
  private subs = new Map<string, Subscription>();

  hypePrice = computed(() => this.hypeData()?.summary?.currentPrice ?? null);
  hypeChange = computed(() => this.hypeData()?.summary?.priceChangePercentage24h ?? null);

  constructor() {
    this.refresh();
    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => {
      clearInterval(intervalID);
      this.hypeSub?.unsubscribe();
      this.subs.forEach(sub => sub.unsubscribe());
    });
  }

  refresh() {
    this.hypeSub?.unsubscribe();
    this.hypeSub = this.api
      .getData('hype')
      .subscribe({
        next: (data) => this.hypeData.set(data),
        error: () => this.hypeData.set(null),
      });

    for (const id of this.assetIds) {
      this.subs.get(id)?.unsubscribe();
      const sub = this.api
        .getAsset(id)
        .subscribe({
          next: (data) => this.assets.update(map => ({ ...map, [id]: data })),
          error: () => this.assets.update(map => ({ ...map, [id]: null })),
        });
      this.subs.set(id, sub);
    }
  }
}
