import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HypeSupplyDistribution } from './hype-supply-distribution';

describe('HypeSupplyDistribution', () => {
  let component: HypeSupplyDistribution;
  let fixture: ComponentFixture<HypeSupplyDistribution>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeSupplyDistribution],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeSupplyDistribution);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
