import { Component, computed, effect, input, signal } from '@angular/core';
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
  currentPrice = input<number | null>(null);
  stakingApr = input<number | null>(null);

  // Champs utilisateur
  hypeProjectionAmount = signal<number | null>(null);
  projectedPriceValue = signal<number | null>(null);
  aprInput = signal<number | null>(null);

  constructor() {
    effect(() => {
      const apiApr = this.stakingApr();
      if (this.aprInput() === null && apiApr !== null) {
        this.aprInput.set(this.roundApr(apiApr));
      }
    });
  }

  setApr(value: number | null) {
    if (value === null || Number.isNaN(value)) {
      this.aprInput.set(null);
      return;
    }
    this.aprInput.set(this.roundApr(value));
  }

  private roundApr(value: number) {
    return Math.round(value * 100) / 100;
  }

  projectedPricePlaceholder = computed(() => {
    const price = this.currentPrice();
    return price !== null ? String(price) : '';
  });

  effectiveAprPercent = computed(() => {
    const custom = this.aprInput();
    if (custom !== null && Number.isFinite(custom) && custom > 0) {
      return custom;
    }
    const api = this.stakingApr();
    if (api !== null && Number.isFinite(api) && api > 0) {
      return api;
    }
    return null;
  });

  // Conversion de l'apr en nombre pour les calculs
  stakingAprRaw = computed(() => {
    const apr = this.effectiveAprPercent();
    if (apr === null) {
      return null;
    }
    return apr / 100;
  });

  // On calcule le prix effectif, soit c'est celui donné par l'user soir le prix actuel si pas de valeur
  effectivePrice = computed(() => {
    const projected = this.projectedPriceValue();
    const current = this.currentPrice();
    return projected !== null && projected !== undefined && projected > 0 ? projected : current;
  });

  // Calcul de la valeur du pf avec les datas données
  projectedValueUsd = computed(() => {
    if (this.effectivePrice() == null) {
      return null;
    }
    return formatNumber((this.hypeProjectionAmount() ?? 0) * this.effectivePrice()!);
  });

  // Rewards annuels bruts
  private yearlyRaw = computed(() => {
    if (this.stakingAprRaw() === null) {
      return null;
    }
    return (this.hypeProjectionAmount() ?? 0) * this.stakingAprRaw()!;
  });

  // Les calculs des rewards selon le temps
  hypeProjectionYearly = computed(() => (this.yearlyRaw() !== null ? formatNumber(this.yearlyRaw()!) : null));
  hypeProjectionMonthly = computed(() =>
    this.yearlyRaw() !== null ? formatNumber(this.yearlyRaw()! / 12) : null,
  );
  hypeProjectionWeekly = computed(() =>
    this.yearlyRaw() !== null ? formatNumber(this.yearlyRaw()! / 52) : null,
  );
  hypeProjectionDaily = computed(() =>
    this.yearlyRaw() !== null ? formatNumber(this.yearlyRaw()! / 365) : null,
  );

  // les calculs de la valeur du bag selon le temps
  hypeProjectionYearlyUsd = computed(() => {
    if (this.yearlyRaw() === null || this.effectivePrice() == null) {
      return null;
    }
    return formatNumber(this.yearlyRaw()! * this.effectivePrice()!);
  });
  hypeProjectionMonthlyUsd = computed(() => {
    if (this.yearlyRaw() === null || this.effectivePrice() == null) {
      return null;
    }
    return formatNumber((this.yearlyRaw()! / 12) * this.effectivePrice()!);
  });
  hypeProjectionWeeklyUsd = computed(() => {
    if (this.yearlyRaw() === null || this.effectivePrice() == null) {
      return null;
    }
    return formatNumber((this.yearlyRaw()! / 52) * this.effectivePrice()!);
  });
  hypeProjectionDailyUsd = computed(() => {
    if (this.yearlyRaw() === null || this.effectivePrice() == null) {
      return null;
    }
    return formatNumber((this.yearlyRaw()! / 365) * this.effectivePrice()!);
  });
}
