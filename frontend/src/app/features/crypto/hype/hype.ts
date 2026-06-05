import { Component, inject, computed, signal, DestroyRef } from '@angular/core';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';
import { DailyChart } from '../../../shared/components/daily-chart/daily-chart';
import type { HypeDto } from '../../../core/models';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import { formatNumber } from '../../../core/services/format-number';
import type { MetricCard } from './hype-metric-card/metric-card-model-hype';
import { HypeMetricCard } from './hype-metric-card/hype-metric-card';
import { HypeBurnCard } from './hype-burn-card/hype-burn-card';
import { HypeFluxChart } from './hype-flux-chart/hype-flux-chart';
import { HypeActivityChart } from './hype-activity-chart/hype-activity-chart';
import { HypeSupplyDistribution } from './hype-supply-distribution/hype-supply-distribution';
import { FormHypeProjection } from './form-hype-projection/form-hype-projection';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

function formatMetric(value: number | null, suffix = ''): string {
  return value !== null ? formatNumber(value) + suffix : '-';
}

@Component({
  selector: 'app-hype',
  imports: [
    AssetMainCard,
    PriceChart,
    DailyChart,
    HypeMetricCard,
    HypeBurnCard,
    HypeFluxChart,
    HypeActivityChart,
    HypeSupplyDistribution,
    FormHypeProjection,
  ],
  templateUrl: './hype.html',
  styleUrl: './hype.css',
})
export class Hype {
  private destroyRef = inject(DestroyRef);

  private api = inject(DashboardApiService);

  data = signal<HypeDto | null>(null);

  // Summary
  currentPrice = computed(() => this.data()?.summary.currentPrice ?? null);
  priceChangePercentage24h = computed(() => this.data()?.summary.priceChangePercentage24h ?? null);
  totalVolume = computed(() => this.data()?.summary.totalVolume ?? null);
  marketCap = computed(() => this.data()?.summary.marketCap ?? null);
  symbol = computed(() => this.data()?.summary.symbol ?? 'HYPE');
  lastRefresh = computed(() => this.data()?.summary.lastRefresh ?? null);

  // Charts
  historyPrices = computed(() => this.data()?.charts.historyPrices ?? []);
  historyDays = computed(() => this.data()?.charts.historyDays ?? []);
  livePrices = computed(() => this.data()?.charts.livePrices ?? []);
  liveDays = computed(() => this.data()?.charts.liveDays ?? []);
  activityVolume = computed(() => this.data()?.charts.activityVolume ?? []);
  activityOpenInterest = computed(() => this.data()?.charts.activityOpenInterest ?? []);
  activityDays = computed(() => this.data()?.charts.activityDays ?? []);

  // Timed Data
  burned24h = computed(() => this.data()?.timedData.burned24h ?? null);
  volatVolume = computed(() => this.data()?.timedData.volatVolume ?? null);
  volatOpenInterest = computed(() => this.data()?.timedData.volatOpenInterest ?? null);
  volatHlpProvider = computed(() => this.data()?.timedData.volatHlpProvider ?? null);
  fluxBurned = computed(() => this.data()?.timedData.fluxBurned ?? []);
  fluxIssued = computed(() => this.data()?.timedData.fluxIssued ?? []);
  fluxNetFlow = computed(() => this.data()?.timedData.fluxNetFlow ?? []);
  fluxDays = computed(() => this.data()?.timedData.fluxDays ?? []);
  burned30d = computed(() => this.data()?.timedData.burned30d ?? null);
  circulating30d = computed(() => this.data()?.timedData.circulating30d ?? null);
  flux30d = computed(() => this.data()?.timedData.flux30d ?? null);

  // Supply
  circulatingSupply = computed(() => this.data()?.supply.circulatingSupply ?? null);
  maxSupply = computed(() => this.data()?.supply.maxSupply ?? null);
  hypeBurned100 = computed(() => this.data()?.supply.hypeBurned100 ?? null);
  circulating100 = computed(() => this.data()?.supply.circulating100 ?? null);

  // Blockchain
  bridgedHype = computed(() => this.data()?.blockchain.bridgedHype ?? null);
  ratioBridged = computed(() => this.data()?.blockchain.ratioBridged ?? null);
  liquidStaked = computed(() => this.data()?.blockchain.liquidStaked ?? null);
  stakedEvmCore = computed(() => this.data()?.blockchain.stakedEvmCore ?? null);

  // HLP
  providerTvl = computed(() => this.data()?.hlp.providerTvl ?? null);
  providerApr = computed(() => this.data()?.hlp.providerApr ?? null);
  ratioProvider = computed(() => this.data()?.hlp.ratioProvider ?? null);

  // Valuation
  totalStakedHype = computed(() => this.data()?.valuation.totalStakedHype ?? null);
  ratioStaked = computed(() => this.data()?.valuation.ratioStaked ?? null);
  stakingApr = computed(() => this.data()?.valuation.stakingApr ?? null);
  fdv = computed(() => this.data()?.valuation.fdv ?? null);
  ratioMcapFdv = computed(() => this.data()?.valuation.ratioMcapFdv ?? null);
  dailyVolume = computed(() => this.data()?.valuation.dailyVolume ?? null);
  openInterest = computed(() => this.data()?.valuation.openInterest ?? null);
  feesDaily = computed(() => this.data()?.valuation.feesDaily ?? null);
  feesAnnual = computed(() => this.data()?.valuation.feesAnnual ?? null);
  ratioPriceFees = computed(() => this.data()?.valuation.ratioPriceFees ?? null);
  ratioOImcap = computed(() => this.data()?.valuation.ratioOImcap ?? null);

  // Metric cards
  cards = computed<MetricCard[]>(() => [
    {
      title: 'Supply',
      metrics: [
        { label: 'Circulating', value: formatMetric(this.circulatingSupply()) },
        { label: 'Maximum', value: formatMetric(this.maxSupply()) },
        { label: 'Burned', value: formatMetric(this.hypeBurned100(), '%') },
      ],
    },

    {
      title: 'Valuation',
      metrics: [
        { label: 'Market Cap', value: formatMetric(this.marketCap(), '$') },
        { label: 'FDV', value: formatMetric(this.fdv(), '$') },
        { label: 'MCap / FDV', value: formatMetric(this.ratioMcapFdv(), 'x') },
      ],
    },

    {
      title: 'Issuance (Daily Avg)',
      metrics: [
        { label: 'Burn (30D avg)', value: formatMetric(this.burned30d()) },
        { label: 'Issued (30D avg)', value: formatMetric(this.circulating30d()) },
        { label: 'Net (30D avg)', value: formatMetric(this.flux30d()) },
      ],
    },

    {
      title: 'EVM Network',
      metrics: [
        { label: 'Bridged', value: formatMetric(this.bridgedHype()) },
        { label: 'Supply Share', value: formatMetric(this.ratioBridged()) },
        {
          label: 'Liquid Staked',
          value: formatMetric(this.liquidStaked()),
          ratio: this.stakedEvmCore() !== null ? formatMetric(this.stakedEvmCore(), '%') : null,
        },
      ],
    },

    {
      title: 'Generated Fees',
      metrics: [
        { label: '24h (Est.)', value: formatMetric(this.feesDaily()) },
        { label: 'Annualized (Est.)', value: formatMetric(this.feesAnnual()) },
        { label: 'Price / Fees Ratio', value: formatMetric(this.ratioPriceFees()) },
      ],
    },

    {
      title: 'Staking',
      metrics: [
        { label: 'Total Staked', value: formatMetric(this.totalStakedHype()) },
        { label: 'Staked Ratio', value: formatMetric(this.ratioStaked(), '%') },
        { label: 'Avg. APR', value: formatMetric(this.stakingApr(), '%') },
      ],
    },

    {
      title: 'Activity',
      metrics: [
        {
          label: '24h Volume',
          value: formatMetric(this.dailyVolume(), '$'),
          variation: this.volatVolume() !== null ? this.volatVolume() : null,
        },
        {
          label: 'Open Interest',
          value: formatMetric(this.openInterest(), '$'),
          variation: this.volatOpenInterest(),
        },
        { label: 'OI / MCap', value: formatMetric(this.ratioOImcap(), 'x') },
      ],
    },

    {
      title: 'HL Provider',
      metrics: [
        {
          label: 'TVL',
          value: formatMetric(this.providerTvl(), '$'),
          variation: this.volatHlpProvider(),
        },
        {
          label: 'Current APR',
          value:
            this.providerApr() !== null
              ? formatMetric(this.providerApr()! * 100, '%')
              : '-',
          colorClass:
            this.providerApr() !== null && this.providerApr()! >= 0
              ? 'text-green-400'
              : 'text-red-400',
        },
        { label: 'Vol / TVL', value: formatMetric(this.ratioProvider(), 'x') },
      ],
    },
  ]);

  // Constructeur — initialisation du refresh et du timer
  constructor() {
    this.refresh();
    const intervalID = setInterval(() => this.refresh(), 180000);
    this.destroyRef.onDestroy(() => clearInterval(intervalID));
  }

  // TODO: Message d'erreur si on reçoit plus rien, la dernière donnée valide reste mais plus de mise à jour. Appartition tag rouge "erreur donnée plus a jour"
  refresh() {
    this.api
      .getData('hype')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((data: HypeDto | null) => this.data.set(data));
  }
}
