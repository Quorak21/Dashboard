import { Component, inject, computed, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import type { HypeDto, AssetDto, RegisteredAssetDto } from '../../core/models';
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

  registeredAssets = signal<RegisteredAssetDto[]>([]);
  private hypeData = signal<HypeDto | null>(null);
  
  // Dynamic assets map
  assets = signal<Record<string, AssetDto | null>>({});

  private hypeSub?: Subscription;
  private subs = new Map<string, Subscription>();

  hypePrice = computed(() => this.hypeData()?.summary?.currentPrice ?? null);
  hypeChange = computed(() => this.hypeData()?.summary?.priceChangePercentage24h ?? null);

  constructor() {
    this.loadRegisteredAssets();
    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => {
      clearInterval(intervalID);
      this.hypeSub?.unsubscribe();
      this.subs.forEach(sub => sub.unsubscribe());
    });
  }

  loadRegisteredAssets() {
    this.api
      .getRegisteredAssets()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((assets) => {
        if (assets) {
          this.registeredAssets.set(assets);
          this.refresh();
        }
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

    const currentAssets = this.registeredAssets();
    for (const asset of currentAssets) {
      this.subs.get(asset.id)?.unsubscribe();
      const sub = this.api
        .getAsset(asset.id)
        .subscribe({
          next: (data) => this.assets.update(map => ({ ...map, [asset.id]: data })),
          error: () => this.assets.update(map => ({ ...map, [asset.id]: null })),
        });
      this.subs.set(asset.id, sub);
    }
  }

  getCurrencySymbol(currency: string): string {
    switch (currency) {
      case 'SEK': return 'SEK';
      case 'GBP': return '£';
      case 'USD': return '$';
      default: return currency;
    }
  }
}
