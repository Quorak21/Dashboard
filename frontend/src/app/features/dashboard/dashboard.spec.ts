import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import type { HypeDto, InveBDto } from '../../core/models';
import { Dashboard } from './dashboard';
import { DashboardApiService } from '../../core/services/dashboard-api.service';

describe('Mon Premier Test Dashboard', () => {
  const hypeDto = {
    summary: { currentPrice: 1.25, priceChangePercentage24h: 5.42 },
  } as HypeDto;

  const invebDto = { currentPrice: 245.5, priceChangePercentage24h: -1.25 } as InveBDto;

  const getData = vi.fn((cle: string) => {
    if (cle === 'hype') {
      return of(hypeDto);
    }
    if (cle === 'inveb') {
      return of(invebDto);
    }
    return of(null);
  });

  const fauxServiceApi = { getData };

  beforeEach(() => {
    getData.mockClear();
  });

  it('devrait bien recevoir et stocker les prix de Hype et Inveb', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    const composant = fixture.componentInstance;

    fixture.detectChanges();

    expect(composant.hypePrice()).toBe(1.25);
    expect(composant.invebPrice()).toBe(245.5);
    expect(composant.hypeChange()).toBe(5.42);
    expect(composant.invebChange()).toBe(-1.25);
  });

  it('devrait appeler getData pour hype et inveb au mount', () => {
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [{ provide: DashboardApiService, useValue: fauxServiceApi }, provideRouter([])],
    });

    const fixture = TestBed.createComponent(Dashboard);
    fixture.detectChanges();

    expect(getData).toHaveBeenCalledWith('hype');
    expect(getData).toHaveBeenCalledWith('inveb');
    expect(getData).toHaveBeenCalledTimes(2);
  });
});
