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
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
