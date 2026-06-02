import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { HypeSupplyDistribution } from './hype-supply-distribution';

describe('HypeSupplyDistribution', () => {
  let component: HypeSupplyDistribution;
  let fixture: ComponentFixture<HypeSupplyDistribution>;

  beforeAll(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  });

  afterAll(() => {
    vi.restoreAllMocks();
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeSupplyDistribution],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeSupplyDistribution);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('computes circulating, burned and unissued supply correctly', () => {
    fixture.componentRef.setInput('circulatingSupply', '300000000');
    fixture.componentRef.setInput('maxSupply', '900000000');
    fixture.detectChanges();

    expect(component.supplyData()).toEqual([300000000, 100000000, 600000000]);
  });

  it('falls back to defaults when inputs are invalid', () => {
    fixture.componentRef.setInput('circulatingSupply', '');
    fixture.componentRef.setInput('maxSupply', '');
    fixture.detectChanges();

    expect(component.supplyData()).toEqual([0, 0, 1000000000]);
  });
});
