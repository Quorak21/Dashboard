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
  price = input<number | null>(null);

  logo = computed(() => this.asset() === "placeholder" ? "assets/logos/placeholder.png" : "assets/logos/" + this.asset() + ".png");
  name = computed(() => this.asset() === "placeholder" ? "À venir !" : this.asset().toUpperCase());
  link = computed(() => this.asset() === "placeholder" ? "/dashboard" : "/" + this.asset());

}
