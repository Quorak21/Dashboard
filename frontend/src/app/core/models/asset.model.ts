export interface AssetModel {
    id: string;
    name: string;
    symbol: string;
    logoPath: string;
    type: 'CRYPTO' | 'STOCK' | 'ETF';
    currentPrice: number;
    variation24h: number;
    volume24h: number;
}
