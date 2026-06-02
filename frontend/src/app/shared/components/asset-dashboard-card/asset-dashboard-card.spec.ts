import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AssetDashboardCard } from './asset-dashboard-card';

describe('AssetDashboardCard', () => {
  let component: AssetDashboardCard;
  let fixture: ComponentFixture<AssetDashboardCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetDashboardCard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetDashboardCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('asset', 'hype');
    fixture.componentRef.setInput('title', 'Hyperliquid');
    fixture.componentRef.setInput('price', 12.34);
    fixture.componentRef.setInput('change', 5.67);
    fixture.componentRef.setInput('currencySymbol', '$');
    await fixture.whenStable();
  });

  it('builds logo path and route link from asset input', () => {
    expect(component.logo()).toBe('assets/logos/hype.png');
    expect(component.link()).toBe('/hype');
  });

  it('renders title, price and positive variation', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Hyperliquid');
    expect(text).toContain('12.34 $');
    expect(text).toContain('+5.67%');
  });
});
