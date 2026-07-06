import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';
import { QuarterlyAlertsBanner } from './quarterly-alerts-banner';
import { DashboardApiService } from '../../../core/services/dashboard-api.service';
import type { QuarterlyAlertsResponse } from '../../../core/models';

describe('QuarterlyAlertsBanner', () => {
  let fixture: ComponentFixture<QuarterlyAlertsBanner>;
  const mockAlertsResponse: QuarterlyAlertsResponse = {
    alerts: [
      {
        assetId: 'inveb',
        displayName: 'Investor AB',
        label: 'Investor AB',
        updatedAt: '2026-04-15',
        daysStale: 92,
      },
      {
        assetId: 'o',
        displayName: 'Realty',
        label: 'Realty',
        updatedAt: '2026-03-10',
        daysStale: 110,
      }
    ]
  };

  const getQuarterlyAlerts = vi.fn();
  const fauxServiceApi = { getQuarterlyAlerts };

  beforeEach(() => {
    getQuarterlyAlerts.mockReset();
  });

  async function configureAndCreate() {
    await TestBed.configureTestingModule({
      imports: [QuarterlyAlertsBanner],
      providers: [
        { provide: DashboardApiService, useValue: fauxServiceApi },
        provideRouter([]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(QuarterlyAlertsBanner);
    fixture.detectChanges();
  }

  it('displays the banner with French text when there are active alerts', async () => {
    getQuarterlyAlerts.mockReturnValue(of(mockAlertsResponse));
    await configureAndCreate();

    const element: HTMLElement = fixture.nativeElement;
    
    // Check signal data
    expect(fixture.componentInstance.alerts().length).toBe(2);
    expect(fixture.componentInstance.alerts()[0].displayName).toBe('Investor AB');

    // DOM Assertions
    const bannerContainer = element.querySelector('.bg-copper-700\\/10');
    expect(bannerContainer).toBeTruthy();
    expect(element.textContent).toContain('Fondamentaux à vérifier :');
    expect(element.textContent).toContain('Investor AB');
    expect(element.textContent).toContain('Realty');
    
    // Link assertions
    const links = element.querySelectorAll('a');
    expect(links.length).toBe(2);
    expect(links[0].getAttribute('href')).toBe('/inveb');
    expect(links[1].getAttribute('href')).toBe('/o');
  });

  it('does not display the banner when alerts list is empty', async () => {
    getQuarterlyAlerts.mockReturnValue(of({ alerts: [] }));
    await configureAndCreate();

    const element: HTMLElement = fixture.nativeElement;
    expect(fixture.componentInstance.alerts().length).toBe(0);

    const bannerContainer = element.querySelector('.bg-copper-700\\/10');
    expect(bannerContainer).toBeFalsy();
  });

  it('does not display the banner and handles API error gracefully', async () => {
    getQuarterlyAlerts.mockReturnValue(throwError(() => new Error('API failure')));
    await configureAndCreate();

    const element: HTMLElement = fixture.nativeElement;
    expect(fixture.componentInstance.alerts().length).toBe(0);

    const bannerContainer = element.querySelector('.bg-copper-700\\/10');
    expect(bannerContainer).toBeFalsy();
  });
});
