import { Component, inject, computed, signal, DestroyRef } from '@angular/core';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';
import { DailyChart } from '../../../shared/components/daily-chart/daily-chart';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import { formatNumber } from '../../../core/services/format-number';
import { formatTime } from '../../../core/services/format-dates';
import type { MetricCard } from './hype-metric-card/metric-card-model-hype';
import { HypeMetricCard } from './hype-metric-card/hype-metric-card';
import { HypeBurnCard } from './hype-burn-card/hype-burn-card';
import { HypeFluxChart } from './hype-flux-chart/hype-flux-chart';
import { HypeSupplyDistribution } from './hype-supply-distribution/hype-supply-distribution';
import { FormHypeProjection } from './form-hype-projection/form-hype-projection';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-hype',
  imports: [AssetMainCard, PriceChart, DailyChart, HypeMetricCard, HypeBurnCard, HypeFluxChart, HypeSupplyDistribution, FormHypeProjection],
  templateUrl: './hype.html',
  styleUrl: './hype.css',
})
export class Hype {

  public formatNumber = formatNumber;
  public formatTime = formatTime;
  private destroyRef = inject(DestroyRef);
  
  private api = inject(DashboardApiService);

  // TODO: Changer le any pour eviter les fausses données
  data = signal<any>(null);

  // Summary
  currentPrice = computed(() => this.data()?.summary.currentPrice ?? null);
  priceChangePercentage24h = computed(() => this.data()?.summary.priceChangePercentage24h ?? null);
  totalVolume = computed(() => this.data()?.summary.totalVolume ?? null);
  marketCap = computed(() => this.data()?.summary.marketCap ?? null);
  symbol = computed(() => this.data()?.summary.symbol ?? 'HYPE');
  lastRefresh = computed(() => this.data()?.summary.lastRefresh ?? 0);

  // Charts
  historyPrices = computed(() => this.data()?.charts.historyPrices ?? []);
  historyDays = computed(() => this.data()?.charts.historyDays ?? []);
  livePrices = computed(() => this.data()?.charts.livePrices ?? []);
  liveDays = computed(() => this.data()?.charts.liveDays ?? []);

  // Timed Data
  burned24h = computed(() => this.data()?.timedData.burned24h ?? 0);
  volatVolume = computed(() => this.data()?.timedData.volatVolume ?? 0);
  volatOpenInterest = computed(() => this.data()?.timedData.volatOpenInterest ?? 0);
  volatHlpProvider = computed(() => this.data()?.timedData.volatHlpProvider ?? 0);  
  fluxBurned = computed(() => this.data()?.timedData.fluxBurned ?? []);
  fluxIssued = computed(() => this.data()?.timedData.fluxIssued ?? []);
  fluxNetFlow = computed(() => this.data()?.timedData.fluxNetFlow ?? []);
  fluxDays = computed(() => this.data()?.timedData.fluxDays ?? []);
  burned30d = computed(() => this.data()?.timedData.burned30d ?? '');
  circulating30d = computed(() => this.data()?.timedData.circulating30d ?? '');
  flux30d = computed(() => this.data()?.timedData.flux30d ?? '');

  // Supply
  circulatingSupply = computed(() => this.data()?.supply.circulatingSupply ?? '');
  maxSupply = computed(() => this.data()?.supply.maxSupply ?? '');
  hypeBurned100 = computed(() => this.data()?.supply.hypeBurned100 ?? '');
  circulating100 = computed(() => this.data()?.supply.circulating100 ?? '');

  // Blockchain
  bridgedHype = computed(() => this.data()?.blockchain.bridgedHype ?? '');
  ratioBridged = computed(() => this.data()?.blockchain.ratioBridged ?? '');
  liquidStaked = computed(() => this.data()?.blockchain.liquidStaked ?? '');
  stakedEvmCore = computed(() => this.data()?.blockchain.stakedEvmCore ?? '');

  // HLP
  providerTvl = computed(() => this.data()?.hlp.providerTvl ?? '');
  providerApr = computed(() => this.data()?.hlp.providerApr ?? '');
  ratioProvider = computed(() => this.data()?.hlp.ratioProvider ?? '');

  // Valuation
  totalStakedHype = computed(() => this.data()?.valuation.totalStakedHype ?? '');
  ratioStaked = computed(() => this.data()?.valuation.ratioStaked ?? '');
  stakingApr = computed(() => this.data()?.valuation.stakingApr ?? '');
  fdv = computed(() => this.data()?.valuation.fdv ?? '');
  ratioMcapFdv = computed(() => this.data()?.valuation.ratioMcapFdv ?? '');
  dailyVolume = computed(() => this.data()?.valuation.dailyVolume ?? '');
  openInterest = computed(() => this.data()?.valuation.openInterest ?? '');
  feesDaily = computed(() => this.data()?.valuation.feesDaily ?? '');
  feesAnnual = computed(() => this.data()?.valuation.feesAnnual ?? '');
  ratioPriceFees = computed(() => this.data()?.valuation.ratioPriceFees ?? '');
  ratioOImcap = computed(() => this.data()?.valuation.ratioOImcap ?? '');

  // Metric cards
  cards = computed<MetricCard[]>(() => [

    {
      title: "Supply",
      metrics: [
        { label: "Circulating", value: this.formatNumber(this.circulatingSupply()) },
        { label: "Maximum", value: this.formatNumber(this.maxSupply()) },
        { label: "Burned", value: this.formatNumber(this.hypeBurned100()) + "%" }
      ],
    },

    {
      title: "Valuation",
      metrics: [
        { label: "Market Cap", value: this.formatNumber(this.marketCap()) + "$" },
        { label: "FDV", value: this.formatNumber(this.fdv()) + "$" },
        { label: "MCap / FDV", value: this.formatNumber(this.ratioMcapFdv()) + "x" }
      ],
    },

    {
      title: "Issuance (Daily Avg)",
      metrics: [
        { label: "Burn (30D avg)", value: this.formatNumber(this.burned30d()) },
        { label: "Issued (30D avg)", value: this.formatNumber(this.circulating30d()) },
        { label: "Net (30D avg)", value: this.formatNumber(this.flux30d()) }
      ],
    },

    {
      title: "EVM Network",
      metrics: [
        { label: "Bridged", value: this.formatNumber(this.bridgedHype()) },
        { label: "Supply Share", value: this.formatNumber(this.ratioBridged()) },
        { label: "Liquid Staked", value: this.formatNumber(this.liquidStaked()), ratio: this.formatNumber(this.stakedEvmCore()) + "%" }
      ],
    },

    {
      title: "Generated Fees",
      metrics: [
        { label: "24h (Est.)", value: this.formatNumber(this.feesDaily()) },
        { label: "Annualized (Est.)", value: this.formatNumber(this.feesAnnual()) },
        { label: "Price / Fees Ratio", value: this.formatNumber(this.ratioPriceFees()) }
      ],
    },

    {
      title: "Staking",
      metrics: [
        { label: "Total Staked", value: this.formatNumber(this.totalStakedHype()) },
        { label: "Staked Ratio", value: this.formatNumber(this.ratioStaked()) + "%" },
        { label: "Avg. APR", value: this.formatNumber(this.stakingApr()) + "%" }
      ],
    },

    {
      title: "Activity",
      metrics: [
        { label: "24h Volume", value: this.formatNumber(this.dailyVolume()) + "$", variation: this.volatVolume() },
        { label: "Open Interest", value: this.formatNumber(this.openInterest()) + "$", variation: this.volatOpenInterest() },
        { label: "OI / MCap", value: this.formatNumber(this.ratioOImcap()) + "x" }
      ],
    },

    {
      title: "HL Provider",
      metrics: [
        { label: "TVL", value: this.formatNumber(this.providerTvl()) + "$", variation: this.volatHlpProvider() },
        {
          label: "Current APR",
          value: this.formatNumber(Number(this.providerApr()) * 100) + "%",
          colorClass: Number(this.providerApr()) >= 0 ? "text-green-400" : "text-red-400"
        },
        { label: "Vol / TVL", value: this.formatNumber(this.ratioProvider()) + "x" }
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
    this.api.getData('hype').pipe(takeUntilDestroyed(this.destroyRef)).subscribe(data => this.data.set(data));
  }

}
