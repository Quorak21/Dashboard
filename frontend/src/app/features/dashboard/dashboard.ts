import { Component, inject, computed, signal, DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import type { HypeDto, InveBDto } from '../../core/models';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';
import { DashboardApiService } from '../../core/services/dashboard-api.service';
import { LucideAngularModule, Zap, TrendingUp, Landmark, Boxes } from 'lucide-angular';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard, LucideAngularModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard {
  readonly Zap = Zap;
  readonly TrendingUp = TrendingUp;
  readonly Landmark = Landmark;
  readonly Boxes = Boxes;

  private api = inject(DashboardApiService);
  private destroyRef = inject(DestroyRef);

  private hypeData = signal<HypeDto | null>(null);
  private invebData = signal<InveBDto | null>(null);

  hypePrice = computed(() => this.hypeData()?.summary?.currentPrice ?? null);
  invebPrice = computed(() => this.invebData()?.currentPrice ?? null);

  hypeChange = computed(() => this.hypeData()?.summary?.priceChangePercentage24h ?? null);
  invebChange = computed(() => this.invebData()?.priceChangePercentage24h ?? null);

  constructor() {
    this.refresh();
    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => clearInterval(intervalID));
  }

  refresh() {
    this.api
      .getData('hype')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => this.hypeData.set(data));
    this.api
      .getData('inveb')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data) => this.invebData.set(data));
  }
}
