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
    fixture.componentRef.setInput('stakingApr', 12);
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

  it('keeps projected price placeholder empty when current price is missing', () => {
    fixture.componentRef.setInput('currentPrice', null);
    fixture.detectChanges();

    expect(component.projectedPricePlaceholder()).toBe('');
  });

  it('rounds APR input to two decimal places', () => {
    component.setApr(12.3456);

    expect(component.aprInput()).toBe(12.35);
    expect(component.effectiveAprPercent()).toBe(12.35);
  });

  it('computes projections from manual APR when API APR is missing', () => {
    fixture.componentRef.setInput('stakingApr', null);
    component.aprInput.set(10);
    component.hypeProjectionAmount.set(100);
    component.projectedPriceValue.set(5);
    fixture.detectChanges();

    expect(component.effectiveAprPercent()).toBe(10);
    expect(component.hypeProjectionYearly()).toBe('10');
    expect(component.projectedValueUsd()).toBe('500');
  });
});
