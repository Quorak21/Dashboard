import { Component, input, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { formatNumber } from '../../../../core/services/format-number';
import { LucideAngularModule, Flame } from 'lucide-angular';
import { FormatNumberPipe } from '../../../../shared/pipes/format-number.pipe';

@Component({
  selector: 'app-hype-burn-card',
  imports: [DecimalPipe, LucideAngularModule, FormatNumberPipe],
  templateUrl: './hype-burn-card.html',
})
export class HypeBurnCard {
  readonly Flame = Flame;

  burned24h = input<number | null>(null);
  hypeBurned100 = input<number | null>(null);
  currentPrice = input<number | null>(null);

  // Valeur en USD calculée
  burnedValueUsd = computed(() => this.currentPrice() !== null ? formatNumber((this.burned24h() ?? 0) * this.currentPrice()!) : null);

  // Longueur de l'arc de l'ellipse M 10 50 A 35 50 0 0 1 100 50
  totalLength = 174;

  dashArrayStyle = computed(() => {
    const percent = this.hypeBurned100() ?? 0;
    const ratio = Math.max(0, Math.min(1, percent / 100));

    // Le premier chiffre est la partie visible (remplie), le deuxième est le vide (la partie restante)
    const filledLength = this.totalLength * ratio;
    return `${filledLength} ${this.totalLength}`;
  });
}
