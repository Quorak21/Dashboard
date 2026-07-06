export type AssetType = 'STOCK' | 'REIT' | 'ETF' | 'TRUST';
export type PriceSource = 'FMP' | 'SCRAPE' | 'CACHE';
export type MarketStatus = 'OPEN' | 'CLOSED';

export interface DividendHistoryEntry {
  year: number;
  amount: number;
  currency: string;
}

export interface DividendsBlock {
  forwardDividend: number | null;
  forwardDividendCurrency: string | null;
  frequency: string | null;
  estimatedYield?: number;
  avgDividendGrowth10Y?: number;
  history: DividendHistoryEntry[];
}

export interface HoldingEntry {
  name: string;
  weightPercent: number;
}

export interface SectorWeight {
  sector: string;
  weightPercent: number;
}

export interface FundamentalsBlock {
  updatedAt: string;
  source: string;
  stale: boolean;
  metrics: Record<string, unknown>;
  topHoldings?: HoldingEntry[];
  sectorWeights?: SectorWeight[];
}

export interface AssetDto {
  assetId: string;
  symbol: string;
  displayName: string | null;
  type: AssetType | null;
  currency: string | null;
  currentPrice: number | null;
  marketCap: number | null;
  priceChangePercentage24h: number | null;
  totalVolume: number | null;
  /** Epoch milliseconds */
  lastRefresh: number | null;
  priceSource: PriceSource | null;
  marketStatus: MarketStatus | null;
  historyPrices: number[];
  historyDays: number[];
  livePrices: number[];
  liveDays: number[];
  dividends: DividendsBlock | null;
  fundamentals: FundamentalsBlock | null;
}

export interface StaleAssetAlert {
  assetId: string;
  displayName: string;
  label: string;
  updatedAt: string;
  daysStale: number;
}

export interface QuarterlyAlertsResponse {
  alerts: StaleAssetAlert[];
}
