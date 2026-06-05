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
    fixture.componentRef.setInput('burned24h', 100);
    fixture.componentRef.setInput('hypeBurned100', 12.5);
    fixture.componentRef.setInput('currentPrice', 2);
    await fixture.whenStable();
  });

  it('computes burned value in USD from burned amount and price', () => {
    expect(component.burnedValueUsd()).toBe('200');
  });

  it('computes gauge stroke-dasharray from burned percentage', () => {
    expect(component.dashArrayStyle()).toBe('21.75 174');
  });

  it('caps dasharray when percentage is below zero', () => {
    fixture.componentRef.setInput('hypeBurned100', -5);
    fixture.detectChanges();

    expect(component.dashArrayStyle()).toBe('0 174');
  });

  it('returns null burned value when current price is missing', () => {
    fixture.componentRef.setInput('currentPrice', null);
    fixture.detectChanges();

    expect(component.burnedValueUsd()).toBeNull();
  });
});
