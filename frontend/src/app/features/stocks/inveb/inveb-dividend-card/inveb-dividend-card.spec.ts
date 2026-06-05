import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InvebDividendCard } from './inveb-dividend-card';

describe('InvebDividendCard', () => {
  let fixture: ComponentFixture<InvebDividendCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvebDividendCard],
    }).compileComponents();

    fixture = TestBed.createComponent(InvebDividendCard);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders skeleton section labels', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Dividend');
    expect(text).toContain('10Y Avg Growth');
    expect(text).toContain('Last 5 Years');
    expect(text).toContain('Next Projection');
    expect(text).toContain('Est. Yield');
  });

  it('computes estimated yield from current price and projected dividend', () => {
    fixture.componentRef.setInput('hasData', true);
    fixture.componentRef.setInput('currentPrice', 300);
    fixture.detectChanges();

    expect(fixture.componentInstance.estimatedYield()).toBeCloseTo(2, 2);
    const text = fixture.nativeElement.textContent as string;
    expect(fixture.componentInstance.estimatedYieldLabel()).toBe('2%');
    expect(text).toContain('2%');
  });

  it('shows dashes when live data is unavailable', () => {
    fixture.componentRef.setInput('hasData', false);
    fixture.componentRef.setInput('currentPrice', null);
    fixture.detectChanges();

    expect(fixture.componentInstance.avgGrowth10Y()).toBe('-');
    expect(fixture.componentInstance.projectionDividendSek()).toBe('-');
    expect(fixture.componentInstance.historyRows().every((row) => row.amount === '-')).toBe(true);
    expect(fixture.componentInstance.estimatedYield()).toBeNull();
  });
});
