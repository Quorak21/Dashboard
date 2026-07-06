import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AssetDashboardCard } from './asset-dashboard-card';

describe('AssetDashboardCard', () => {
  let fixture: ComponentFixture<AssetDashboardCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetDashboardCard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetDashboardCard);
    fixture.componentRef.setInput('asset', 'inveb');
    fixture.componentRef.setInput('title', 'Investor AB');
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('does not render market badge when marketStatus is absent', () => {
    expect(fixture.componentInstance.showMarketBadge()).toBe(false);
    expect(fixture.nativeElement.querySelector('[aria-label]')).toBeNull();
  });

  it('renders green badge when market is open', () => {
    fixture.componentRef.setInput('marketStatus', 'OPEN');
    fixture.detectChanges();

    const badge: HTMLElement | null = fixture.nativeElement.querySelector('[aria-label="Marché ouvert"]');
    expect(badge).not.toBeNull();
    expect(badge?.classList.contains('bg-green-500')).toBe(true);
  });

  it('renders red badge when market is closed', () => {
    fixture.componentRef.setInput('marketStatus', 'CLOSED');
    fixture.detectChanges();

    const badge: HTMLElement | null = fixture.nativeElement.querySelector('[aria-label="Marché fermé"]');
    expect(badge).not.toBeNull();
    expect(badge?.classList.contains('bg-red-500')).toBe(true);
  });
});
