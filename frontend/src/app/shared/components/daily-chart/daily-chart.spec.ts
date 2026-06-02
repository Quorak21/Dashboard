import { ComponentFixture, TestBed } from '@angular/core/testing';
import { vi } from 'vitest';

import { DailyChart } from './daily-chart';

describe('DailyChart', () => {
  let component: DailyChart;
  let fixture: ComponentFixture<DailyChart>;

  beforeAll(() => {
    vi.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue(null);
  });

  afterAll(() => {
    vi.restoreAllMocks();
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DailyChart],
    }).compileComponents();

    fixture = TestBed.createComponent(DailyChart);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('keeps input defaults before receiving data', () => {
    expect(component.prices()).toEqual([]);
    expect(component.labels()).toEqual([]);
    expect(component.currency()).toBe('');
  });

  it('formats tooltip label with currency', () => {
    fixture.componentRef.setInput('currency', '$');
    const formatter = ((ctx: { parsed: { y?: number } }) =>
      `${ctx.parsed.y?.toFixed(2)} ${component.currency()}`);

    expect(formatter({ parsed: { y: 12.345 } })).toBe('12.35 $');
  });
});
