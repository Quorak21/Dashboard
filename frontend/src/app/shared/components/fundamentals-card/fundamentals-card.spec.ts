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
    expect(element.textContent).toContain('No holdings available');
    // Footer source should be omitted (since it's empty / '-')
    expect(element.querySelector('p')?.textContent).toBeUndefined();
  });
});
