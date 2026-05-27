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

  // Computed — Market data
  currentPrice = computed(() => this.data()?.currentPrice ?? null);
  priceChangePercentage24h = computed(() => this.data()?.priceChangePercentage24h ?? null);
  totalVolume = computed(() => this.data()?.totalVolume ?? null);
  marketCap = computed(() => this.data()?.marketCap ?? null);
  symbol = computed(() => this.data()?.symbol ?? 'HYPE');
  lastRefresh = computed(() => this.data()?.lastRefresh ?? 0);

  // Computed — Charts annuel et daily
  historyPrices = computed(() => this.data()?.historyPrices ?? []);
  historyDays = computed(() => this.data()?.historyDays ?? []);
  livePrices = computed(() => this.data()?.livePrices ?? []);
  liveDays = computed(() => this.data()?.liveDays ?? []);

  // Computed — Chart flux
  fluxBurned = computed(() => this.data()?.fluxBurned ?? []);
  fluxIssued = computed(() => this.data()?.fluxIssued ?? []);
  fluxNetFlow = computed(() => this.data()?.fluxNetFlow ?? []);
  fluxDays = computed(() => this.data()?.fluxDays ?? []);

  // Computed — API Hyperliquid
  circulatingSupply = computed(() => this.data()?.circulatingSupply ?? '');
  totalValueLocked = computed(() => this.data()?.totalValueLocked ?? '');
  apr = computed(() => this.data()?.apr ?? '');
  dailyVolume = computed(() => this.data()?.dailyVolume ?? '');
  ratioProvider = computed(() => this.data()?.ratioProvider ?? '');
  openInterest = computed(() => this.data()?.openInterest ?? '');
  feesDaily = computed(() => this.data()?.feesDaily ?? '');
  feesAnnual = computed(() => this.data()?.feesAnnual ?? '');
  volatVolume = computed(() => this.data()?.volatVolume ?? 0);
  volatOpenInterest = computed(() => this.data()?.volatOpenInterest ?? 0);
  volatHlpProvider = computed(() => this.data()?.volatHlpProvider ?? 0);
  stakingApr = computed(() => this.data()?.stakingApr ?? '');
  maxSupply = computed(() => this.data()?.maxSupply ?? '');
  circulation100 = computed(() => this.data()?.circulation100 ?? '');
  fdv = computed(() => this.data()?.fdv ?? '');
  ratioMcapFdv = computed(() => this.data()?.ratioMcapFdv ?? '');
  hypeBurned100 = computed(() => this.data()?.hypeBurned100 ?? '');
  ratioPriceFees = computed(() => this.data()?.ratioPriceFees ?? '');
  ratioOImcap = computed(() => this.data()?.ratioOImcap ?? '');
  totalStakedHype = computed(() => this.data()?.totalStakedHype ?? '');
  ratioStaked = computed(() => this.data()?.ratioStaked ?? '');
  burned30d = computed(() => this.data()?.burned30d ?? '');
  circulating30d = computed(() => this.data()?.circulating30d ?? '');
  flux30d = computed(() => this.data()?.flux30d ?? '');
  burned24h = computed(() => this.data()?.burned24h ?? 0);

  // Computed — Blockchain
  bridgedHype = computed(() => this.data()?.bridgedHype ?? '');
  ratioBridged = computed(() => this.data()?.ratioBridged ?? '');
  liquidStaked = computed(() => this.data()?.liquidStaked ?? '');
  stakedEvmCore = computed(() => this.data()?.stakedEvmCore ?? '');

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
        { label: "TVL", value: this.formatNumber(this.totalValueLocked()) + "$", variation: this.volatHlpProvider() },
        {
          label: "Current APR",
          value: this.formatNumber(Number(this.apr()) * 100) + "%",
          colorClass: Number(this.apr()) >= 0 ? "text-green-400" : "text-red-400"
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
