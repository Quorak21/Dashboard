import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PriceFreshnessBadge } from './price-freshness-badge';

describe('PriceFreshnessBadge', () => {
  let fixture: ComponentFixture<PriceFreshnessBadge>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PriceFreshnessBadge],
    }).compileComponents();

    fixture = TestBed.createComponent(PriceFreshnessBadge);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders "Marché Fermé" when marketStatus is CLOSED', () => {
    fixture.componentRef.setInput('marketStatus', 'CLOSED');
    fixture.componentRef.setInput('priceSource', 'FMP');
    fixture.componentRef.setInput('lastRefresh', Date.now());
    fixture.detectChanges();

    expect(fixture.componentInstance.freshness().type).toBe('closed');
    expect(fixture.componentInstance.freshness().label).toBe('Marché Fermé');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.rounded-full')?.textContent).toContain('Marché Fermé');
  });

  it('renders "Différé" when priceSource is CACHE', () => {
    fixture.componentRef.setInput('marketStatus', 'OPEN');
    fixture.componentRef.setInput('priceSource', 'CACHE');
    fixture.componentRef.setInput('lastRefresh', Date.now());
    fixture.detectChanges();

    expect(fixture.componentInstance.freshness().type).toBe('stale');
    expect(fixture.componentInstance.freshness().label).toBe('Différé');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.rounded-full')?.textContent).toContain('Différé');
  });

  it('renders "Donnée Obsolète" when data age exceeds 2× sync interval', () => {
    fixture.componentRef.setInput('marketStatus', 'OPEN');
    fixture.componentRef.setInput('priceSource', 'FMP');
    fixture.componentRef.setInput('syncIntervalMinutes', 15);
    // 40 minutes ago (> 2 × 15 min)
    fixture.componentRef.setInput('lastRefresh', Date.now() - 40 * 60 * 1000);
    fixture.detectChanges();

    expect(fixture.componentInstance.freshness().type).toBe('stale');
    expect(fixture.componentInstance.freshness().label).toBe('Donnée Obsolète');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.rounded-full')?.textContent).toContain('Donnée Obsolète');
  });

  it('renders "En Direct" when age is within 2× sync interval', () => {
    fixture.componentRef.setInput('marketStatus', 'OPEN');
    fixture.componentRef.setInput('priceSource', 'FMP');
    fixture.componentRef.setInput('syncIntervalMinutes', 15);
    // 20 minutes ago (< 2 × 15 min)
    fixture.componentRef.setInput('lastRefresh', Date.now() - 20 * 60 * 1000);
    fixture.detectChanges();

    expect(fixture.componentInstance.freshness().type).toBe('live');
    expect(fixture.componentInstance.freshness().label).toBe('En Direct');
  });

  it('renders "En Direct" when marketStatus is OPEN, source is FMP, and age is fresh', () => {
    fixture.componentRef.setInput('marketStatus', 'OPEN');
    fixture.componentRef.setInput('priceSource', 'FMP');
    fixture.componentRef.setInput('syncIntervalMinutes', 15);
    // 5 minutes ago
    fixture.componentRef.setInput('lastRefresh', Date.now() - 5 * 60 * 1000);
    fixture.detectChanges();

    expect(fixture.componentInstance.freshness().type).toBe('live');
    expect(fixture.componentInstance.freshness().label).toBe('En Direct');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.rounded-full')?.textContent).toContain('En Direct');
  });

  it('renders "En Direct" when marketStatus is OPEN, source is SCRAPE, and age is fresh', () => {
    fixture.componentRef.setInput('marketStatus', 'OPEN');
    fixture.componentRef.setInput('priceSource', 'SCRAPE');
    // 5 minutes ago
    fixture.componentRef.setInput('lastRefresh', Date.now() - 5 * 60 * 1000);
    fixture.detectChanges();

    expect(fixture.componentInstance.freshness().type).toBe('live');
    expect(fixture.componentInstance.freshness().label).toBe('En Direct');

    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('.rounded-full')?.textContent).toContain('En Direct');
  });
});
