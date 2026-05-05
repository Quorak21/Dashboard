import { Component, input, computed } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { formatNumber } from '../../services/format-number';

@Component({
  selector: 'app-hype-burn-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './hype-burn-card.html',
})
export class HypeBurnCard {
  public formatNumber = formatNumber;
  burned24h = input.required<number>();
  hypeBurned = input<string>('0');

  // Longueur de l'arc de l'ellipse M 10 50 A 35 50 0 0 1 100 50
  totalLength = 174;
  
  dashArrayStyle = computed(() => {
    const burnedStr = this.hypeBurned() || '0';
    const percent = parseFloat(burnedStr);
    const validPercent = isNaN(percent) ? 0 : percent;
    // ratio is out of 100
    let ratio = validPercent / 100;
    if (ratio > 1) ratio = 1;
    if (ratio < 0) ratio = 0;
    
    // Le premier chiffre est la partie visible (remplie), le deuxième est le vide (la partie restante)
    const filledLength = this.totalLength * ratio;
    return `${filledLength} ${this.totalLength}`;
  });
}
