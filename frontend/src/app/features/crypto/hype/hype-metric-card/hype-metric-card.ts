import { Component, Input } from '@angular/core';
import { MetricCard } from '../metric-card-model-hype';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-hype-metric-card',
  imports: [DecimalPipe],
  templateUrl: './hype-metric-card.html',
  styleUrl: './hype-metric-card.css',
})
export class HypeMetricCard {
  @Input({ required: true }) card!: MetricCard;
}
