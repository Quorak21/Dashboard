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

  it('renders fundamentals metrics and source', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Fundamentals');
    expect(text).toContain('Key Metrics');
    expect(text).toContain('5Y Avg Discount');
    expect(text).toContain('15%');
    expect(text).toContain('Patricia Industries Weight');
    expect(text).toContain('Holdings');
    expect(text).toContain('ABB');
    expect(text).toContain('AstraZeneca');
    expect(text).toContain('Mölnlycke');
    expect(text).toContain('Nasdaq');
    expect(text).toContain('Saab');
    expect(text).toContain('rapport 1er trimestre 2026');
  });
});
