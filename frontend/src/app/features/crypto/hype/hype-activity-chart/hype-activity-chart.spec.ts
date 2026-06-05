import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { HypeActivityChart } from './hype-activity-chart';

describe('HypeActivityChart', () => {
  let component: HypeActivityChart;
  let fixture: ComponentFixture<HypeActivityChart>;

  beforeAll(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  });

  afterAll(() => {
    vi.restoreAllMocks();
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeActivityChart],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeActivityChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('hasData is false when labels are empty', () => {
    fixture.componentRef.setInput('labels', []);
    expect(component.hasData()).toBe(false);
  });

  it('hasData is true when labels are provided', () => {
    fixture.componentRef.setInput('labels', [1_700_000_000_000]);
    fixture.componentRef.setInput('volume', [1000]);
    fixture.componentRef.setInput('openInterest', [5000]);
    expect(component.hasData()).toBe(true);
  });
});
