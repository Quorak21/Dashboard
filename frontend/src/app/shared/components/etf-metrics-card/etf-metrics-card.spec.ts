import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EtfMetricsCard } from './etf-metrics-card';
import type { FundamentalsBlock } from '../../../core/models';

describe('EtfMetricsCard', () => {
  let fixture: ComponentFixture<EtfMetricsCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtfMetricsCard],
    }).compileComponents();

    fixture = TestBed.createComponent(EtfMetricsCard);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders detailed data when fundamentals block is populated', () => {
    const mockFundamentals: FundamentalsBlock = {
      updatedAt: '2025-05-31',
      source: 'Source: iShares ETF Factsheet',
      stale: false,
      metrics: {
        'management-fee': '0.65%',
        'total-assets': '$2.8bn',
        'nav-discount-premium': '0.02%',
      },
      topHoldings: [
        { name: 'NextEra Energy', weightPercent: 6.1 },
        { name: 'Southern Company', weightPercent: 3.9 },
      ],
      sectorWeights: [],
    };

    fixture.componentRef.setInput('fundamentals', mockFundamentals);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hasData()).toBe(true);
    expect(component.sourceLabel()).toBe('Source: iShares ETF Factsheet');
    expect(component.ter()).toBe('0.65%');
    expect(component.aum()).toBe('$2.8bn');
    expect(component.navPremium()).toBe('0.02%');
    expect(component.topHoldings().length).toBe(2);
    expect(component.topHoldings()[0]).toEqual({ name: 'NextEra Energy', weight: '6.10%' });

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h3')?.textContent).toContain('ETF Metrics');
    expect(element.textContent).toContain('Management Fee (TER)');
    expect(element.textContent).toContain('0.65%');
    expect(element.textContent).toContain('NextEra Energy');
    expect(element.textContent).toContain('6.10%');
    expect(element.textContent).toContain('Source: iShares ETF Factsheet');
  });

  it('renders fallback messages when data is unavailable', () => {
    fixture.componentRef.setInput('fundamentals', null);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hasData()).toBe(false);
    expect(component.ter()).toBe('-');
    expect(component.aum()).toBe('-');
    expect(component.navPremium()).toBe('-');
    expect(component.topHoldings().length).toBe(0);

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('No metrics available');
    expect(element.textContent).toContain('No holdings available');
    expect(element.querySelector('p')?.textContent).toBeUndefined();
  });
});
