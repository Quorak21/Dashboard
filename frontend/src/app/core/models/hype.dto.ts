export interface HypeSummaryDto {
  symbol: string;
  currentPrice: number | null;
  marketCap: number | null;
  priceChangePercentage24h: number | null;
  totalVolume: number | null;
  lastRefresh: number | null;
}

export interface HypeChartsDto {
  historyPrices: number[];
  historyDays: number[];
  livePrices: number[];
  liveDays: number[];
  activityVolume: number[];
  activityOpenInterest: number[];
  activityDays: number[];
}

export interface HypeTimedDataDto {
  burned24h: number | null;
  volatVolume: number | null;
  volatOpenInterest: number | null;
  volatHlpProvider: number | null;
  burned30d: number | null;
  circulating30d: number | null;
  flux30d: number | null;
  fluxBurned: number[];
  fluxIssued: number[];
  fluxNetFlow: number[];
  fluxDays: number[];
}

export interface HypeSupplyDto {
  circulatingSupply: number | null;
  maxSupply: number | null;
  hypeBurned100: number | null;
  circulating100: number | null;
}

export interface HypeBlockchainDto {
  bridgedHype: number | null;
  liquidStaked: number | null;
  ratioBridged: number | null;
  stakedEvmCore: number | null;
}

export interface HypeHlpDto {
  providerTvl: number | null;
  providerApr: number | null;
  ratioProvider: number | null;
}

export interface HypeValuationDto {
  fdv: number | null;
  ratioMcapFdv: number | null;
  ratioOImcap: number | null;
  dailyVolume: number | null;
  openInterest: number | null;
  feesDaily: number | null;
  feesAnnual: number | null;
  ratioPriceFees: number | null;
  stakingApr: number | null;
  totalStakedHype: number | null;
  ratioStaked: number | null;
}

export interface HypeDto {
  summary: HypeSummaryDto;
  charts: HypeChartsDto;
  timedData: HypeTimedDataDto;
  supply: HypeSupplyDto;
  blockchain: HypeBlockchainDto;
  hlp: HypeHlpDto;
  valuation: HypeValuationDto;
}
