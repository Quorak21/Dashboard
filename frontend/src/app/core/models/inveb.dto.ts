export interface InveBDto {
  symbol: string;
  currentPrice: number | null;
  marketCap: number | null;
  priceChangePercentage24h: number | null;
  totalVolume: number | null;
  lastRefresh: number | null;
  historyPrices: number[];
  historyDays: number[];
  livePrices: number[];
  liveDays: number[];
}
