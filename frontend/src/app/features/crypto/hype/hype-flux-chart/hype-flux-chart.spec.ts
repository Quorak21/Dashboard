import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HypeFluxChart } from './hype-flux-chart';

describe('HypeFluxChart', () => {
  let component: HypeFluxChart;
  let fixture: ComponentFixture<HypeFluxChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeFluxChart],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeFluxChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
