import { Component, computed, input } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { formatNumber } from '../../services/format-number';

@Component({
  selector: 'app-form-hype-projection',
  imports: [ReactiveFormsModule,],
  templateUrl: './form-hype-projection.html',
  styleUrl: './form-hype-projection.css',
})
export class FormHypeProjection {

  currentPrice = input<number>(0);
  stakingApr = input<string>('0');

  stakingAprRaw = computed(() => Number(this.stakingApr()) / 100);

  get stakingAprStr(): string {
    return formatNumber(this.stakingAprRaw() * 100);
  }

  hypeProjection = new FormControl<number | null>(null);
  hypeProjectionAmount = toSignal(this.hypeProjection.valueChanges, { initialValue: null });

  priceProjection = new FormControl<number | null>(null);
  projectedPriceValue = toSignal(this.priceProjection.valueChanges, { initialValue: null });

  effectivePrice = computed(() => {
    const projected = this.projectedPriceValue();
    const current = this.currentPrice();
    return projected !== null && projected !== undefined && projected > 0 ? projected : current;
  });

  projectedValueUsd = computed(() => formatNumber((this.hypeProjectionAmount() || 0) * this.effectivePrice()));

  private yearlyRaw = computed(() => (this.hypeProjectionAmount() || 0) * this.stakingAprRaw());

  hypeProjectionYearly = computed(() => formatNumber(this.yearlyRaw()));
  hypeProjectionMonthly = computed(() => formatNumber(this.yearlyRaw() / 12));
  hypeProjectionWeekly = computed(() => formatNumber(this.yearlyRaw() / 52));
  hypeProjectionDaily = computed(() => formatNumber(this.yearlyRaw() / 365));

  hypeProjectionYearlyUsd = computed(() => formatNumber(this.yearlyRaw() * this.effectivePrice()));
  hypeProjectionMonthlyUsd = computed(() => formatNumber((this.yearlyRaw() / 12) * this.effectivePrice()));
  hypeProjectionWeeklyUsd = computed(() => formatNumber((this.yearlyRaw() / 52) * this.effectivePrice()));
  hypeProjectionDailyUsd = computed(() => formatNumber((this.yearlyRaw() / 365) * this.effectivePrice()));

}