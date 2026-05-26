import { Component, input, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { formatNumber } from '../../../../core/services/format-number';
import { LucideAngularModule, Flame } from 'lucide-angular';

@Component({
  selector: 'app-hype-burn-card',
  standalone: true,
  imports: [DecimalPipe, LucideAngularModule],
  templateUrl: './hype-burn-card.html',
})
export class HypeBurnCard {
  readonly Flame = Flame;

  public formatNumber = formatNumber;

  burned24h = input.required<number>();
  hypeBurned100 = input<string>('0');
  currentPrice = input<number>(0);

  // Valeur en USD calculée
  burnedValueUsd = computed(() => formatNumber(this.burned24h() * this.currentPrice()));

  // Longueur de l'arc de l'ellipse M 10 50 A 35 50 0 0 1 100 50
  totalLength = 174;

  dashArrayStyle = computed(() => {
    // On recup le string pour le changer en nombre
    const burnedStr = this.hypeBurned100() || '0';
    const percent = parseFloat(burnedStr);
    const validPercent = isNaN(percent) ? 0 : percent;
    // On check que le ratio est entre 0 et 1
    const ratio = Math.max(0, Math.min(1, validPercent / 100));

    // Le premier chiffre est la partie visible (remplie), le deuxième est le vide (la partie restante)
    const filledLength = this.totalLength * ratio;
    return `${filledLength} ${this.totalLength}`;
  });
}
