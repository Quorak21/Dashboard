import { Component, inject, signal, DestroyRef } from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import type { StaleAssetAlert } from '../../../core/models';
import { LucideAngularModule, AlertCircle } from 'lucide-angular';

@Component({
  selector: 'app-quarterly-alerts-banner',
  standalone: true,
  imports: [RouterLink, LucideAngularModule],
  templateUrl: './quarterly-alerts-banner.html',
  styleUrl: './quarterly-alerts-banner.css',
})
export class QuarterlyAlertsBanner {
  readonly AlertCircle = AlertCircle;

  private api = inject(DashboardApiService);
  private destroyRef = inject(DestroyRef);

  alerts = signal<StaleAssetAlert[]>([]);

  constructor() {
    this.fetchAlerts();
  }

  private fetchAlerts() {
    this.api
      .getQuarterlyAlerts()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (response && Array.isArray(response.alerts)) {
            this.alerts.set(response.alerts);
          } else {
            this.alerts.set([]);
          }
        },
        error: () => {
          this.alerts.set([]);
        }
      });
  }
}
