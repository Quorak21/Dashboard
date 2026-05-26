import { Component, input } from '@angular/core';
import { formatTime } from '../../../core/services/format-dates';
import { formatNumber } from '../../../core/services/format-number';
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

  asset = input<string>();
  symbol = input<string>("");
  tag = input<string>("");
  change24h = input<number>(0);
  volume24h = input<number>(0);
  marketCap = input<number>(0);
  actualPrice = input<number>(0);
  lastRefresh = input<number>(0);
  currencySymbol = input<string>('$');
}
