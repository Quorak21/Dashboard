import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DividendCard } from './dividend-card';
import type { DividendsBlock } from '../../../core/models';

describe('DividendCard', () => {
  let fixture: ComponentFixture<DividendCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DividendCard],
    }).compileComponents();

    fixture = TestBed.createComponent(DividendCard);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should display detailed data and compute estimated yield when dividends block is populated', () => {
    const mockDividends: DividendsBlock = {
      forwardDividend: 6.0,
      forwardDividendCurrency: 'SEK',
      frequency: 'Annual',
      estimatedYield: 2.0,
      avgDividendGrowth10Y: 8.4,
      history: [
        { year: 2017, amount: 4.0, currency: 'SEK' },
        { year: 2018, amount: 4.1, currency: 'SEK' },
        { year: 2019, amount: 4.2, currency: 'SEK' },
        { year: 2020, amount: 4.3, currency: 'SEK' },
        { year: 2021, amount: 4.4, currency: 'SEK' },
        { year: 2022, amount: 4.5, currency: 'SEK' },
        { year: 2023, amount: 4.6, currency: 'SEK' },
        { year: 2024, amount: 4.7, currency: 'SEK' },
        { year: 2025, amount: 4.8, currency: 'SEK' },
        { year: 2026, amount: 5.2, currency: 'SEK' },
      ],
    };

    fixture.componentRef.setInput('dividends', mockDividends);
    fixture.componentRef.setInput('currentYearInput', 2026);
    fixture.componentRef.setInput('currency', 'SEK');
    fixture.detectChanges();

    expect(fixture.componentInstance.hasData()).toBe(true);
    expect(fixture.componentInstance.avgGrowth10Y()).toBe('2.96 %');
    expect(fixture.componentInstance.projectionYear()).toBe('2027');
    expect(fixture.componentInstance.projectionDividend()).toBe('6 SEK');
    expect(fixture.componentInstance.estimatedYieldLabel()).toBe('2%');
    expect(fixture.componentInstance.historyRows().length).toBe(10);
    expect(fixture.componentInstance.historyRows()[0]).toEqual({ year: 2026, amount: '5.20 SEK' });

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h3')?.textContent).toContain('Dividend');
    expect(element.querySelector('.text-lg')?.textContent).toContain('2.96 %');
    const listItems = element.querySelectorAll('li');
    expect(listItems.length).toBe(10);
    expect(listItems[0].textContent).toContain('2026');
    expect(listItems[0].textContent).toContain('5.20 SEK');
    expect(element.querySelector('.text-3xl')?.textContent).toContain('6 SEK');
  });

  it('shows dashes when dividends data is unavailable', () => {
    fixture.componentRef.setInput('dividends', null);
    fixture.componentRef.setInput('currentYearInput', 2026);
    fixture.detectChanges();

    expect(fixture.componentInstance.hasData()).toBe(false);
    expect(fixture.componentInstance.avgGrowth10Y()).toBe('-');
    expect(fixture.componentInstance.projectionYear()).toBe('-');
    expect(fixture.componentInstance.projectionDividend()).toBe('-');
    expect(fixture.componentInstance.historyRows().every((row) => row.amount === '-')).toBe(true);
    expect(fixture.componentInstance.estimatedYieldLabel()).toBeNull();

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    const listItems = element.querySelectorAll('li');
    expect(listItems.length).toBe(10);
    expect(listItems[0].textContent).toContain('2026');
    expect(listItems[0].textContent).toContain('-');
    expect(element.querySelector('.text-3xl')?.textContent).toContain('-');
  });
});
