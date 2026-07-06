import { TestBed, ComponentFixture } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi, type Mock } from 'vitest';
import { AssetPage } from './asset-page';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import type { AssetDto } from '../../../core/models';

describe('AssetPage', () => {
  let getAssetSpy: Mock;
  let fixture: ComponentFixture<AssetPage>;

  const mockAsset: AssetDto = {
    assetId: 'inveb',
    symbol: 'INVE-B',
    displayName: 'Investor AB',
    type: 'STOCK',
    currency: 'SEK',
    currentPrice: 250,
    marketCap: 750000000,
    priceChangePercentage24h: 1.5,
    totalVolume: 5000000,
    lastRefresh: Date.now(),
    priceSource: 'FMP',
    marketStatus: 'OPEN',
    historyPrices: [100, 110, 120],
    historyDays: [2021, 2022, 2023],
    livePrices: [248, 249, 250],
    liveDays: [1, 2, 3],
    dividends: {
      forwardDividend: 6.0,
      forwardDividendCurrency: 'SEK',
      frequency: 'Annual',
      estimatedYield: 2.4,
      avgDividendGrowth10Y: 8.5,
      history: [{ year: 2024, amount: 5.6, currency: 'SEK' }],
    },
    fundamentals: {
      updatedAt: '2026-04-15',
      source: 'Q1 2026',
      stale: false,
      metrics: {
        'trailing-pe': 6.11,
      },
      topHoldings: [{ name: 'ABB', weightPercent: 16.5 }],
    },
  };

  beforeAll(() => {
    (globalThis as any).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  });

  beforeEach(async () => {
    getAssetSpy = vi.fn().mockReturnValue(of(mockAsset));

    await TestBed.configureTestingModule({
      imports: [AssetPage],
      providers: [
        {
          provide: DashboardApiService,
          useValue: { getAsset: getAssetSpy },
        },
      ],
    }).compileComponents();
  });

  it('loads and renders the asset data properly', async () => {
    fixture = TestBed.createComponent(AssetPage);
    fixture.componentRef.setInput('assetId', 'inveb');
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(getAssetSpy).toHaveBeenCalledWith('inveb');
    expect(component.data()).not.toBeNull();
    expect(component.assetName()).toBe('Investor AB');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('app-asset-main-card')).toBeTruthy();
    expect(element.querySelector('app-price-chart')).toBeTruthy();
    expect(element.querySelector('app-daily-chart')).toBeTruthy();
    expect(element.querySelector('app-dividend-card')).toBeTruthy();
    expect(element.querySelector('app-fundamentals-card')).toBeTruthy();
    expect(element.querySelector('app-price-freshness-badge')).toBeTruthy();
    fixture.destroy();
  }, 15000);

  it('hides dividend and fundamentals card if data is missing', async () => {
    const mockAssetNoCards: AssetDto = {
      ...mockAsset,
      dividends: null,
      fundamentals: null,
    };
    getAssetSpy.mockReturnValue(of(mockAssetNoCards));

    fixture = TestBed.createComponent(AssetPage);
    fixture.componentRef.setInput('assetId', 'inveb');
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('app-dividend-card')).toBeNull();
    expect(element.querySelector('app-fundamentals-card')).toBeNull();
    fixture.destroy();
  });

  it('renders ETF specific cards instead of fundamentals card if type is ETF', async () => {
    const mockEtfAsset: AssetDto = {
      ...mockAsset,
      type: 'ETF',
      fundamentals: {
        updatedAt: '2025-05-31',
        source: 'iShares Factsheet',
        stale: false,
        metrics: {
          'management-fee': '0.65%',
        },
        topHoldings: [{ name: 'NextEra Energy', weightPercent: 6.1 }],
        sectorWeights: [{ sector: 'Utilities', weightPercent: 61.2 }],
      },
    };
    getAssetSpy.mockReturnValue(of(mockEtfAsset));

    fixture = TestBed.createComponent(AssetPage);
    fixture.componentRef.setInput('assetId', 'infr');
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.isEtf()).toBe(true);

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('app-etf-metrics-card')).toBeTruthy();
    expect(element.querySelector('app-etf-sector-chart')).toBeTruthy();
    expect(element.querySelector('app-fundamentals-card')).toBeNull();
    fixture.destroy();
  });

  it('shows loading indicator while data is being fetched', async () => {
    const { Subject } = await import('rxjs');
    const dataSubject = new Subject<AssetDto | null>();
    getAssetSpy.mockReturnValue(dataSubject.asObservable());

    fixture = TestBed.createComponent(AssetPage);
    fixture.componentRef.setInput('assetId', 'inveb');
    fixture.detectChanges();

    // Data not yet emitted — loading should be true
    const element: HTMLElement = fixture.nativeElement;
    expect(fixture.componentInstance.loading()).toBe(true);
    expect(element.querySelector('app-asset-main-card')).toBeNull();

    // Emit data — loading should clear
    dataSubject.next(mockAsset);
    dataSubject.complete();
    fixture.detectChanges();

    expect(fixture.componentInstance.loading()).toBe(false);
    expect(element.querySelector('app-asset-main-card')).toBeTruthy();
    fixture.destroy();
  });

  it('returns empty display name when API returns null displayName', async () => {
    const mockAssetNoName: AssetDto = {
      ...mockAsset,
      assetId: 'o',
      symbol: 'O',
      displayName: null,
      type: 'REIT',
      currency: 'USD',
    };
    getAssetSpy.mockReturnValue(of(mockAssetNoName));

    fixture = TestBed.createComponent(AssetPage);
    fixture.componentRef.setInput('assetId', 'o');
    fixture.detectChanges();

    expect(fixture.componentInstance.assetName()).toBe('');
    fixture.destroy();
  });
});
