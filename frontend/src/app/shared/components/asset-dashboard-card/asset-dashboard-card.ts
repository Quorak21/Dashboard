import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-asset-dashboard-card',
  imports: [RouterLink, DecimalPipe],
  templateUrl: './asset-dashboard-card.html',
  styleUrl: './asset-dashboard-card.css',
})
export class AssetDashboardCard {

  asset = input.required<string>();
  title = input<string>();
  price = input<number | null>(null);
  change = input<number | null>(null);
  currencySymbol = input<string>('$');

  logo = computed(() => "/assets/logos/" + this.asset() + ".png");
  link = computed(() => this.asset() === 'hype' ? '/hype' : '/asset/' + this.asset());

}
