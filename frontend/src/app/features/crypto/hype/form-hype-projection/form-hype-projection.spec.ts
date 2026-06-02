import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormHypeProjection } from './form-hype-projection';

describe('FormHypeProjection', () => {
  let component: FormHypeProjection;
  let fixture: ComponentFixture<FormHypeProjection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormHypeProjection],
    }).compileComponents();

    fixture = TestBed.createComponent(FormHypeProjection);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('currentPrice', 10);
    fixture.componentRef.setInput('stakingApr', '12');
    await fixture.whenStable();
  });

  it('uses current price when projected price is empty', () => {
    component.hypeProjectionAmount.set(100);

    expect(component.effectivePrice()).toBe(10);
    expect(component.projectedValueUsd()).toBe('1K');
    expect(component.hypeProjectionYearly()).toBe('12');
  });

  it('uses projected price when provided', () => {
    component.hypeProjectionAmount.set(100);
    component.projectedPriceValue.set(20);

    expect(component.effectivePrice()).toBe(20);
    expect(component.projectedValueUsd()).toBe('2K');
    expect(component.hypeProjectionYearlyUsd()).toBe('240');
  });
});
