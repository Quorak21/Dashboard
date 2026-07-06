import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FundamentalsCard } from './fundamentals-card';
import type { FundamentalsBlock } from '../../../core/models';

describe('FundamentalsCard', () => {
  let fixture: ComponentFixture<FundamentalsCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FundamentalsCard],
    }).compileComponents();

    fixture = TestBed.createComponent(FundamentalsCard);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders detailed data when fundamentals block is populated', () => {
    const mockFundamentals: FundamentalsBlock = {
      updatedAt: '2026-04-15',
      source: 'Source: Q1 2026 report',
      stale: false,
      metrics: {
        'trailing-pe': 6.11,
        'debt-leverage': '1.2%',
        'management-cost': '0.09%',
      },
      topHoldings: [
        { name: 'ABB', weightPercent: 16.5 },
        { name: 'Atlas Copco', weightPercent: 15.0 },
      ],
      sectorWeights: [],
    };

    fixture.componentRef.setInput('fundamentals', mockFundamentals);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hasData()).toBe(true);
    expect(component.sourceLabel()).toBe('Source: Q1 2026 report');
    expect(component.metrics().length).toBe(3);
    expect(component.metrics()[0]).toEqual({ label: 'Trailing P/E', value: '6.11' });
    expect(component.metrics()[1]).toEqual({ label: 'Debt Leverage', value: '1.2%' });
    expect(component.topHoldings().length).toBe(2);
    expect(component.topHoldings()[0]).toEqual({ name: 'ABB', weight: '16.50%' });

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h3')?.textContent).toContain('Fundamentals');
    expect(element.textContent).toContain('Trailing P/E');
    expect(element.textContent).toContain('6.11');
    expect(element.textContent).toContain('ABB');
    expect(element.textContent).toContain('16.50%');
    expect(element.textContent).toContain('Source: Q1 2026 report');
  });

  it('renders property-type mix when holdings are absent', () => {
    const mockFundamentals: FundamentalsBlock = {
      updatedAt: '2026-03-31',
      source: 'Realty Income Q1 2026',
      stale: false,
      metrics: {
        'portfolio-occupancy': '98.9%',
      },
      topHoldings: [],
      sectorWeights: [
        { sector: 'Retail', weightPercent: 78.9 },
        { sector: 'Industrial', weightPercent: 15.5 },
      ],
    };

    fixture.componentRef.setInput('fundamentals', mockFundamentals);
    fixture.detectChanges();

    expect(fixture.componentInstance.allocationTitle()).toBe('Portfolio Mix (ABR)');
    expect(fixture.componentInstance.allocationRows()[0]).toEqual({
      name: 'Retail',
      weight: '78.90%',
    });

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Portfolio Mix (ABR)');
    expect(element.textContent).toContain('Retail');
    expect(element.textContent).toContain('78.90%');
  });

  it('renders retail tenant mix when retail industry weights are present', () => {
    const mockFundamentals: FundamentalsBlock = {
      updatedAt: '2026-03-31',
      source: 'Realty Income Q1 2026',
      stale: false,
      metrics: {
        'portfolio-occupancy': '98.9%',
      },
      topHoldings: [],
      sectorWeights: [
        { sector: 'Retail', weightPercent: 78.9 },
        { sector: 'Industrial', weightPercent: 15.5 },
      ],
      retailIndustryWeights: [
        { sector: 'Grocery', weightPercent: 11.0 },
        { sector: 'Convenience Stores', weightPercent: 9.4 },
      ],
    };

    fixture.componentRef.setInput('fundamentals', mockFundamentals);
    fixture.detectChanges();

    expect(fixture.componentInstance.hasRetailIndustryDetail()).toBe(true);
    expect(fixture.componentInstance.retailIndustryWeights()[0]).toEqual({
      name: 'Grocery',
      weight: '11%',
    });

    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('Retail Tenant Mix (ABR)');
    expect(element.textContent).toContain('Grocery');
    expect(element.textContent).toContain('11%');
    expect(element.textContent).toContain('Convenience Stores');
  });

  it('renders fallback messages when live data is unavailable', () => {
    fixture.componentRef.setInput('fundamentals', null);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hasData()).toBe(false);
    expect(component.metrics().length).toBe(0);
    expect(component.topHoldings().length).toBe(0);
    expect(component.sourceLabel()).toBe('-');

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.textContent).toContain('No metrics available');
    expect(element.textContent).toContain('No allocation data available');
    // Footer source should be omitted (since it's empty / '-')
    expect(element.querySelector('p')?.textContent).toBeUndefined();
  });
});
