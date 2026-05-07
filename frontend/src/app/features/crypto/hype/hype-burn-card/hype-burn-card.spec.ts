import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HypeBurnCard } from './hype-burn-card';

describe('HypeBurnCard', () => {
  let component: HypeBurnCard;
  let fixture: ComponentFixture<HypeBurnCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeBurnCard],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeBurnCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
