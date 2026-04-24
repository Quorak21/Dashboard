import { Component, Input } from '@angular/core';
import { formatTime } from '../../../features/crypto/services/format-dates';
import { formatNumber } from '../../../features/crypto/services/format-number';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-asset-main-card',
  imports: [DecimalPipe],
  templateUrl: './asset-main-card.html',
  styleUrl: './asset-main-card.css',
})
export class AssetMainCard {

  public formatTime = formatTime;
  public formatNumber = formatNumber;

  @Input() asset: string = "";
  @Input() symbol: string = "";
  @Input() change24h: number = 0;
  @Input() volume24h: number = 0;
  @Input() marketCap: number = 0;
  @Input() actualPrice: number = 0;
  @Input() lastRefresh: number = 0;
}
