import { Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { LucideAngularModule, TrendingUp, TrendingDown, Clock } from 'lucide-angular';
import { FormatNumberPipe } from '../../pipes/format-number.pipe';
import { FormatTimePipe } from '../../pipes/format-time.pipe';

@Component({
  selector: 'app-asset-main-card',
  imports: [DecimalPipe, LucideAngularModule, FormatNumberPipe, FormatTimePipe],
  templateUrl: './asset-main-card.html',
  styleUrl: './asset-main-card.css',
})
export class AssetMainCard {
  readonly TrendingUp = TrendingUp;
  readonly TrendingDown = TrendingDown;
  readonly Clock = Clock;

  asset = input<string>();
  symbol = input<string>("");
  tag = input<string>("");
  change24h = input<number | null>(null);
  volume24h = input<number | null>(null);
  marketCap = input<number | null>(null);
  actualPrice = input<number | null>(null);
  lastRefresh = input<number | null>(null);
  currencySymbol = input<string>('$');
  marketClosed = input<boolean>(false);
}
