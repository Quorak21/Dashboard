import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
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

  const mockBrwmDto = {
    assetId: 'brwm',
    symbol: 'BRWM',
    currentPrice: 5.50,
    priceChangePercentage24h: 1.5,
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
    if (assetId === 'brwm') {
      return of(mockBrwmDto);
    }
    if (assetId === 'o') {
      return of(mockODto);
    }
    return of(null);
  });

  const fauxServiceApi = { getData, getAsset };

  beforeEach(() => {
    getData.mockClear();
    getAsset.mockClear();
  });

  it('devrait bien recevoir et stocker les prix de Hype, Inveb, Brwm et O', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    const composant = fixture.componentInstance;

    fixture.detectChanges();

    expect(composant.hypePrice()).toBe(1.25);
    expect(composant.assets()['inveb']?.currentPrice).toBe(245.5);
    expect(composant.assets()['brwm']?.currentPrice).toBe(5.50);
    expect(composant.assets()['o']?.currentPrice).toBe(55.0);
    expect(composant.hypeChange()).toBe(5.42);
    expect(composant.assets()['inveb']?.priceChangePercentage24h).toBe(-1.25);
    expect(composant.assets()['brwm']?.priceChangePercentage24h).toBe(1.5);
    expect(composant.assets()['o']?.priceChangePercentage24h).toBe(-0.85);
  });

  it('devrait appeler getData pour hype et getAsset pour inveb/brwm/o au mount', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    expect(getData).toHaveBeenCalledWith('hype');
    expect(getData).toHaveBeenCalledTimes(1);
    expect(getAsset).toHaveBeenCalledWith('inveb');
    expect(getAsset).toHaveBeenCalledWith('brwm');
    expect(getAsset).toHaveBeenCalledWith('o');
    expect(getAsset).toHaveBeenCalledTimes(3);
  });
});

