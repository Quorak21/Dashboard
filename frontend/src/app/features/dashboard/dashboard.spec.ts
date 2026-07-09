import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import type { HypeDto, AssetDto } from '../../core/models';
import { Dashboard } from './dashboard';
import { DashboardApiService } from '../../core/services/dashboard-api.service';

describe('Mon Premier Test Dashboard', () => {
  const hypeDto = {
    summary: { currentPrice: 1.25, priceChangePercentage24h: 5.42 },
  } as HypeDto;

  const mockAssetDto = {
    assetId: 'inveb',
    symbol: 'INVE-B',
    currentPrice: 245.5,
    priceChangePercentage24h: -1.25,
  } as AssetDto;

  const mockODto = {
    assetId: 'o',
    symbol: 'O',
    currentPrice: 55.0,
    priceChangePercentage24h: -0.85,
  } as AssetDto;

  const getData = vi.fn((cle: string) => {
    if (cle === 'hype') {
      return of(hypeDto);
    }
    return of(null);
  });

  const getAsset = vi.fn((assetId: string) => {
    if (assetId === 'inveb') {
      return of(mockAssetDto);
    }
    if (assetId === 'o') {
      return of(mockODto);
    }
    return of(null);
  });

  const mockRegisteredAssets = [
    { id: 'inveb', displayName: 'Investor AB', type: 'STOCK', currency: 'SEK' },
    { id: 'o', displayName: 'Realty', type: 'REIT', currency: 'USD' },
  ];

  const getRegisteredAssets = vi.fn(() => of(mockRegisteredAssets));

  const fauxServiceApi = { getData, getAsset, getRegisteredAssets };

  beforeEach(() => {
    getData.mockClear();
    getAsset.mockClear();
    getRegisteredAssets.mockClear();
  });

  it('devrait bien recevoir et stocker les prix de Hype, Inveb et O', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    const composant = fixture.componentInstance;

    fixture.detectChanges();

    expect(composant.hypePrice()).toBe(1.25);
    expect(composant.assets()['inveb']?.currentPrice).toBe(245.5);
    expect(composant.assets()['o']?.currentPrice).toBe(55.0);
    expect(composant.hypeChange()).toBe(5.42);
    expect(composant.assets()['inveb']?.priceChangePercentage24h).toBe(-1.25);
    expect(composant.assets()['o']?.priceChangePercentage24h).toBe(-0.85);
  });

  it('devrait appeler getData pour hype et getAsset pour inveb/o au mount', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    expect(getData).toHaveBeenCalledWith('hype');
    expect(getData).toHaveBeenCalledTimes(1);
    expect(getRegisteredAssets).toHaveBeenCalledTimes(1);
    expect(getAsset).toHaveBeenCalledWith('inveb');
    expect(getAsset).toHaveBeenCalledWith('o');
    expect(getAsset).toHaveBeenCalledTimes(2);
  });

  it('devrait remettre les prix à null quand les appels API échouent', () => {
    const failingApi = {
      getData: vi.fn(() => throwError(() => new Error('API down'))),
      getAsset: vi.fn(() => throwError(() => new Error('API down'))),
      getRegisteredAssets: vi.fn(() => of(mockRegisteredAssets)),
    };

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: failingApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    const composant = fixture.componentInstance;
    fixture.detectChanges();

    expect(composant.hypePrice()).toBeNull();
    expect(composant.assets()['inveb']).toBeNull();
    expect(composant.assets()['o']).toBeNull();
  });

  it('devrait rafraîchir les données toutes les 3 minutes', () => {
    vi.useFakeTimers();

    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    expect(getData).toHaveBeenCalledTimes(1);
    expect(getAsset).toHaveBeenCalledTimes(2);

    vi.advanceTimersByTime(180000);
    fixture.detectChanges();

    expect(getData).toHaveBeenCalledTimes(2);
    expect(getAsset).toHaveBeenCalledTimes(4);

    vi.useRealTimers();
  });
});
