import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InvebFundamentalsCard } from './inveb-fundamentals-card';

describe('InvebFundamentalsCard', () => {
  let fixture: ComponentFixture<InvebFundamentalsCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InvebFundamentalsCard],
    }).compileComponents();

    fixture = TestBed.createComponent(InvebFundamentalsCard);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders fundamentals metrics and source when data is available', () => {
    fixture.componentRef.setInput('hasData', true);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Fundamentals');
    expect(text).toContain('Key Metrics');
    expect(text).toContain('5Y NAV CAGR');
    expect(text).toContain('15%');
    expect(text).toContain('Atlas Copco');
    expect(text).toContain('Holdings');
    expect(text).toContain('ABB');
    expect(text).toContain('Q1 2026 report');
  });

  it('renders dashes when live data is unavailable', () => {
    fixture.componentRef.setInput('hasData', false);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.metrics().every((metric) => metric.value === '-')).toBe(true);
    expect(component.topHoldings().every((holding) => holding.weight === '-')).toBe(true);
    expect(component.sourceLabel()).toBe('-');
  });
});
