import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { vi, type Mock } from 'vitest';

import { Inveb } from './inveb';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';

describe('Inveb', () => {
  let getDataSpy: Mock;

  beforeAll(() => {
    (globalThis as any).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  });

  beforeEach(async () => {
    getDataSpy = vi.fn().mockReturnValue(of({
      symbol: 'INVE-B',
      currentPrice: 245.5,
      marketCap: 5000,
      priceChangePercentage24h: -1.25,
      totalVolume: 123,
      lastRefresh: Date.UTC(2026, 5, 2, 8, 0, 0),
      historyPrices: [1, 2],
      historyDays: [10, 20],
      livePrices: [3, 4],
      liveDays: [30, 40]
    }));

    await TestBed.configureTestingModule({
      imports: [Inveb],
      providers: [{ provide: DashboardApiService, useValue: { getData: getDataSpy } }]
    }).compileComponents();
  });

  it('maps API payload to component computed signals', async () => {
    const fixture = TestBed.createComponent(Inveb);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    await fixture.whenStable();

    expect(getDataSpy).toHaveBeenCalledWith('inveb');
    expect(component.currentPrice()).toBe(245.5);
    expect(component.symbol()).toBe('INVE-B');
    expect(component.livePrices()).toEqual([3, 4]);
    fixture.destroy();
  });

  it('returns true for market closed when lastRefresh is missing', async () => {
    getDataSpy.mockReturnValue(of(null));
    const fixture = TestBed.createComponent(Inveb);
    const component = fixture.componentInstance;

    fixture.detectChanges();
    await fixture.whenStable();

    expect(component.lastRefresh()).toBe(0);
    expect(component.isMarketClosed()).toBe(true);
    fixture.destroy();
  });
});
