import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssetDashboardCard } from './asset-dashboard-card';

describe('AssetDashboardCard', () => {
  let component: AssetDashboardCard;
  let fixture: ComponentFixture<AssetDashboardCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetDashboardCard],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetDashboardCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
