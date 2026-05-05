import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HypeMetricCard } from './hype-metric-card';

describe('HypeMetricCard', () => {
  let component: HypeMetricCard;
  let fixture: ComponentFixture<HypeMetricCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeMetricCard],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeMetricCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
