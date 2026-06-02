import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { HypeFluxChart } from './hype-flux-chart';

describe('HypeFluxChart', () => {
  let component: HypeFluxChart;
  let fixture: ComponentFixture<HypeFluxChart>;

  beforeAll(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  });

  afterAll(() => {
    vi.restoreAllMocks();
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeFluxChart],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeFluxChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('computes symmetric bounds from net flow values', () => {
    fixture.componentRef.setInput('netFlow', [-5000, 2000, 3000]);
    const bounds = (component as any).getSymmetricBounds();

    expect(bounds).toBe(5125);
  });

  it('returns minimum guard bound when net flow is empty', () => {
    fixture.componentRef.setInput('netFlow', []);
    const bounds = (component as any).getSymmetricBounds();

    expect(bounds).toBeCloseTo(102500, 6);
  });
});
