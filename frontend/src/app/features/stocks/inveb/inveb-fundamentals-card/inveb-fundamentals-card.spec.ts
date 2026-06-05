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

  it('renders dashes when live data is unavailable', () => {
    fixture.componentRef.setInput('hasData', false);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.metrics().every((metric) => metric.value === '-')).toBe(true);
    expect(component.topHoldings().every((holding) => holding.weight === '-')).toBe(true);
    expect(component.sourceLabel()).toBe('-');
  });
});
