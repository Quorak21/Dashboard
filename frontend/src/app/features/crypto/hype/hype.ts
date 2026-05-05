import { Component, inject, OnDestroy, computed } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { AssetMainCard } from '../../../shared/components/asset-main-card/asset-main-card';
import { PriceChart } from '../../../shared/components/price-chart/price-chart';
import { DailyChart } from '../../../shared/components/daily-chart/daily-chart';
import { HttpClient } from '@angular/common/http';
import { formatNumber } from '../services/format-number';
import { formatTime } from '../services/format-dates';
import { signal } from '@angular/core';
import { timer, Subscription } from 'rxjs';
import type { MetricCard } from './metric-card-model-hype';
import { HypeMetricCard } from './hype-metric-card/hype-metric-card';
import { HypeBurnCard } from './hype-burn-card/hype-burn-card';

@Component({
  selector: 'app-hype',
  standalone: true,
  imports: [AssetMainCard, PriceChart, DailyChart, HypeMetricCard, HypeBurnCard],
  templateUrl: './hype.html',
  styleUrl: './hype.css',
})
export class Hype implements OnDestroy {

  public formatNumber = formatNumber;
  public formatTime = formatTime;

  private http = inject(HttpClient);
  private timerSub: Subscription = timer(0, 60000).subscribe(() => this.refresh());
  currentPrice = signal<number>(0);
  priceChangePercentage24h = signal<number>(0);
  totalVolume = signal<number>(0);
  marketCap = signal<number>(0);
  symbol = signal<string>('HYPE');
  lastRefresh = signal<number>(0);

  historyPrices = signal<number[]>([]);
  historyDays = signal<number[]>([]);
  livePrices = signal<number[]>([]);
  liveDays = signal<number[]>([]);

  // Chiffre venant de l'api hyperliquid
  circulatingSupply = signal<string>('');
  totalValueLocked = signal<string>('');
  apr = signal<string>('');
  dailyVolume = signal<string>('');
  ratioProvider = signal<string>('');
  openInterest = signal<string>('');
  feesDaily = signal<string>('');
  feesAnnual = signal<string>('');
  volatVolume = signal<number>(0);
  volatOpenInterest = signal<number>(0);
  volatFees = signal<number>(0);
  volatHlpProvider = signal<number>(0);
  stakingApr = signal<string>('');
  maxSupply = signal<string>('');
  circulation100 = signal<string>('');
  fdv = signal<string>('');
  ratioMcapFdv = signal<string>('');
  hypeBurned = signal<string>('');
  ratioPriceFees = signal<string>('');
  ratioOImcap = signal<string>('');
  totalStakedHype = signal<string>('');
  ratioStaked = signal<string>('');
  burned30d = signal<string>('');
  circulating30d = signal<string>('');
  flux30d = signal<string>('');
  burned24h = signal<number>(0);

  // Chiffre blockchain
  bridgedHype = signal<string>('');
  ratioBridged = signal<string>('');
  liquidStaked = signal<string>('');
  stakedEvmCore = signal<string>('');



  refresh() {
    this.http.get<any>(`${environment.apiUrl}/api/dashboard/hype`).subscribe((data: any) => {
      this.currentPrice.set(data.currentPrice);
      this.priceChangePercentage24h.set(data.priceChangePercentage24h);
      this.totalVolume.set(data.totalVolume);
      this.marketCap.set(data.marketCap);
      this.symbol.set(data.symbol);
      this.lastRefresh.set(data.lastRefresh);
      this.historyPrices.set(data.historyPrices);
      this.historyDays.set(data.historyDays);
      this.livePrices.set(data.livePrices);
      this.liveDays.set(data.liveDays);
      this.circulatingSupply.set(data.circulatingSupply);
      this.totalValueLocked.set(data.totalValueLocked);
      this.apr.set(data.apr);
      this.dailyVolume.set(data.dailyVolume);
      this.ratioProvider.set(data.ratioProvider);
      this.openInterest.set(data.openInterest);
      this.feesDaily.set(data.feesDaily);
      this.feesAnnual.set(data.feesAnnual);
      this.volatVolume.set(data.volatVolume);
      this.volatOpenInterest.set(data.volatOpenInterest);
      this.volatFees.set(data.volatFees);
      this.volatHlpProvider.set(data.volatHlpProvider);
      this.stakingApr.set(data.stakingApr);
      this.maxSupply.set(data.maxSupply);
      this.circulation100.set(data.circulation100);
      this.fdv.set(data.fdv);
      this.ratioMcapFdv.set(data.ratioMcapFdv);
      this.hypeBurned.set(data.hypeBurned);
      this.ratioPriceFees.set(data.ratioPriceFees);
      this.ratioOImcap.set(data.ratioOImcap);
      this.totalStakedHype.set(data.totalStakedHype);
      this.ratioStaked.set(data.ratioStaked);
      this.bridgedHype.set(data.bridgedHype);
      this.ratioBridged.set(data.ratioBridged);
      this.liquidStaked.set(data.liquidStaked);
      this.stakedEvmCore.set(data.stakedEvmCore);
      this.burned30d.set(data.burned30d);
      this.circulating30d.set(data.circulating30d);
      this.flux30d.set(data.flux30d);
      this.burned24h.set(data.burned24h);
    });



  }

  ngOnDestroy() {
    this.timerSub.unsubscribe();
  }

  cards = computed<MetricCard[]>(() => [

    {
      title: "Supply",
      metrics: [
        { label: "Circulating", value: this.formatNumber(Number(this.circulatingSupply())), colorClass: "text-orange-300" },
        { label: "Maximum", value: this.formatNumber(Number(this.maxSupply())), colorClass: "text-orange-300" },
        { label: "Burned", value: this.formatNumber(Number(this.hypeBurned())) + "%", colorClass: "text-blue-400" }
      ],
    },

    {
      title: "Valuation",
      metrics: [
        { label: "Market Cap", value: this.formatNumber(Number(this.marketCap())) + "$", colorClass: "text-emerald-400" },
        { label: "FDV", value: this.formatNumber(Number(this.fdv())) + "$", colorClass: "text-emerald-400" },
        { label: "MCap / FDV", value: this.formatNumber(Number(this.ratioMcapFdv())) + "x", colorClass: "text-amber-400" }
      ],
    },

    {
      title: "Issuance",
      metrics: [
        { label: "30D Burn", value: this.formatNumber(Number(this.burned30d())), colorClass: "text-orange-300" },
        { label: "30D Issued", value: this.formatNumber(Number(this.circulating30d())), colorClass: "text-orange-300" },
        { label: "30D Net", value: this.formatNumber(Number(this.flux30d())), colorClass: "text-orange-300" }
      ],
    },

    {
      title: "EVM Network",
      metrics: [
        { label: "Bridged", value: this.formatNumber(Number(this.bridgedHype())), colorClass: "text-orange-300" },
        { label: "Supply Share", value: this.formatNumber(Number(this.ratioBridged())) + "%", colorClass: "text-blue-400" },
        { label: "Liquid Staked", value: this.formatNumber(Number(this.liquidStaked())), ratio: this.formatNumber(Number(this.stakedEvmCore())) + "%", colorClass: "text-orange-300" }
      ],
    },

    {
      title: "Generated Fees",
      metrics: [
        { label: "24h (Est.)", value: this.formatNumber(Number(this.feesDaily())) + "$", colorClass: "text-emerald-400", variation: this.volatFees() },
        { label: "Annualized", value: this.formatNumber(Number(this.feesAnnual())) + "$", colorClass: "text-emerald-400" },
        { label: "P/F Ratio", value: this.formatNumber(Number(this.ratioPriceFees())) + "x", colorClass: "text-amber-400" }
      ],
    },

    {
      title: "Staking",
      metrics: [
        { label: "Total Staked", value: this.formatNumber(Number(this.totalStakedHype())), colorClass: "text-orange-300" },
        { label: "Staked Ratio", value: this.formatNumber(Number(this.ratioStaked())) + "%", colorClass: "text-blue-400" },
        { label: "Avg. APR", value: this.formatNumber(Number(this.stakingApr())) + "%", colorClass: "text-blue-400" }
      ],
    },

    {
      title: "Activity",
      metrics: [
        { label: "24h Volume", value: this.formatNumber(Number(this.dailyVolume())) + "$", colorClass: "text-emerald-400", variation: this.volatVolume() },
        { label: "Open Interest", value: this.formatNumber(Number(this.openInterest())) + "$", colorClass: "text-emerald-400", variation: this.volatOpenInterest() },
        { label: "OI / MCap", value: this.formatNumber(Number(this.ratioOImcap())) + "x", colorClass: "text-amber-400" }
      ],
    },

    {
      title: "HL Provider",
      metrics: [
        { label: "TVL", value: this.formatNumber(Number(this.totalValueLocked())) + "$", colorClass: "text-emerald-400", variation: this.volatHlpProvider() },
        {
          label: "Current APR",
          value: this.formatNumber(Number(this.apr()) * 100) + "%",
          colorClass: Number(this.apr()) >= 0 ? "text-green-400" : "text-red-400"
        },
        { label: "Vol / TVL", value: this.formatNumber(Number(this.ratioProvider())) + "x", colorClass: "text-amber-400" }
      ],
    },

  ]);



}
