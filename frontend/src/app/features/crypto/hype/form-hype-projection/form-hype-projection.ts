import { Component, computed, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { formatNumber } from '../../../../core/services/format-number';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-form-hype-projection',
  imports: [FormsModule, DecimalPipe],
  templateUrl: './form-hype-projection.html',
  styleUrl: './form-hype-projection.css',
})
export class FormHypeProjection {

  // Récup des datas depuis le parents
  currentPrice = input<number>(0);
  stakingApr = input<string>('0');

  // Conversion de l'apr en nombre pour les calculs
  stakingAprRaw = computed(() => Number(this.stakingApr()) / 100);

  // On créer un signal pour l'input et avoir les résultats live
  hypeProjectionAmount = signal<number | null>(null);
  projectedPriceValue = signal<number | null>(null);

  // On calcule le prix effectif, soit c'est celui donné par l'user soir le prix actuel si pas de valeur
  effectivePrice = computed(() => {
    const projected = this.projectedPriceValue();
    const current = this.currentPrice();
    return projected !== null && projected !== undefined && projected > 0 ? projected : current;
  });

  // Calcul de la valeur du pf avec les datas données
  projectedValueUsd = computed(() => formatNumber((this.hypeProjectionAmount() || 0) * this.effectivePrice()));
  // Rewards annuels bruts
  private yearlyRaw = computed(() => (this.hypeProjectionAmount() || 0) * this.stakingAprRaw());
  // Les calculs des rewards selon le temps
  hypeProjectionYearly = computed(() => formatNumber(this.yearlyRaw()));
  hypeProjectionMonthly = computed(() => formatNumber(this.yearlyRaw() / 12));
  hypeProjectionWeekly = computed(() => formatNumber(this.yearlyRaw() / 52));
  hypeProjectionDaily = computed(() => formatNumber(this.yearlyRaw() / 365));
  // les calculs de la valeur du bag selon le temps
  hypeProjectionYearlyUsd = computed(() => formatNumber(this.yearlyRaw() * this.effectivePrice()));
  hypeProjectionMonthlyUsd = computed(() => formatNumber((this.yearlyRaw() / 12) * this.effectivePrice()));
  hypeProjectionWeeklyUsd = computed(() => formatNumber((this.yearlyRaw() / 52) * this.effectivePrice()));
  hypeProjectionDailyUsd = computed(() => formatNumber((this.yearlyRaw() / 365) * this.effectivePrice()));

}