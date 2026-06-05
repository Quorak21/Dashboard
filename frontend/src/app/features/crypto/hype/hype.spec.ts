import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi, type Mock } from 'vitest';

import type { HypeDto } from '../../../core/models';
import { Hype } from './hype';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';

describe('Hype', () => {
  let getDataSpy: Mock;

  beforeAll(() => {
    (globalThis as any).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  });

  beforeEach(async () => {
    getDataSpy = vi.fn().mockReturnValue(
      of({
        summary: {
          symbol: 'HYPE',
          currentPrice: 1.25,
          priceChangePercentage24h: 5.42,
          marketCap: 123,
          totalVolume: 456,
          lastRefresh: 111,
        },
        charts: {
          historyPrices: [1],
          historyDays: [2],
          livePrices: [3],
          liveDays: [4],
          activityVolume: [1000],
          activityOpenInterest: [5000],
          activityDays: [5],
        },
        timedData: {
          burned24h: 1,
          volatVolume: 2,
          volatOpenInterest: 3,
          volatHlpProvider: 4,
          fluxBurned: [],
          fluxIssued: [],
          fluxNetFlow: [],
          fluxDays: [],
          burned30d: 1,
          circulating30d: 2,
          flux30d: 3,
        },
        supply: { circulatingSupply: 1, maxSupply: 2, hypeBurned100: 3, circulating100: 4 },
        blockchain: { bridgedHype: 1, ratioBridged: 2, liquidStaked: 3, stakedEvmCore: 4 },
        hlp: { providerTvl: 100, providerApr: 0.1, ratioProvider: 1.2 },
        valuation: {
          fdv: 1,
          ratioMcapFdv: 2,
          ratioOImcap: 3,
          dailyVolume: 4,
          openInterest: 5,
          feesDaily: 6,
          feesAnnual: 7,
          ratioPriceFees: 8,
          stakingApr: 9,
          totalStakedHype: 10,
          ratioStaked: 11,
        },
      } satisfies HypeDto),
    );

    await TestBed.configureTestingModule({
      imports: [Hype],
      providers: [{ provide: DashboardApiService, useValue: { getData: getDataSpy } }],
    }).compileComponents();
  });

  it('hydrates key computed signals from API payload', async () => {
    const fixture = TestBed.createComponent(Hype);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    await fixture.whenStable();

    expect(getDataSpy).toHaveBeenCalledWith('hype');
    expect(component.currentPrice()).toBe(1.25);
    expect(component.symbol()).toBe('HYPE');
    expect(component.historyPrices()).toEqual([1]);
    expect(component.activityVolume()).toEqual([1000]);
    expect(component.activityOpenInterest()).toEqual([5000]);
    expect(component.activityDays()).toEqual([5]);
    expect(component.providerApr()).toBe(0.1);
    fixture.destroy();
  });

  it('keeps safe defaults when API returns null', async () => {
    getDataSpy.mockReturnValue(of(null));
    const fixture = TestBed.createComponent(Hype);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.currentPrice()).toBeNull();
    expect(component.symbol()).toBe('HYPE');
    expect(component.historyPrices()).toEqual([]);
    expect(component.fluxBurned()).toEqual([]);
    expect(component.activityVolume()).toEqual([]);
    expect(component.activityOpenInterest()).toEqual([]);
    expect(component.activityDays()).toEqual([]);
    fixture.destroy();
  });
});
