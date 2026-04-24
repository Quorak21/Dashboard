import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DailyChart } from './daily-chart';

describe('DailyChart', () => {
  let component: DailyChart;
  let fixture: ComponentFixture<DailyChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DailyChart],
    }).compileComponents();

    fixture = TestBed.createComponent(DailyChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
