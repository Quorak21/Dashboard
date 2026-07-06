import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EtfSectorChart } from './etf-sector-chart';
import type { SectorWeight } from '../../../core/models';

describe('EtfSectorChart', () => {
  let fixture: ComponentFixture<EtfSectorChart>;

  beforeAll(() => {
    (globalThis as any).ResizeObserver = class {
      observe() {}
      unobserve() {}
      disconnect() {}
    };
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EtfSectorChart],
    }).compileComponents();

    fixture = TestBed.createComponent(EtfSectorChart);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders sector weights when data is populated', () => {
    const mockSectorWeights: SectorWeight[] = [
      { sector: 'Utilities', weightPercent: 61.2 },
      { sector: 'Energy', weightPercent: 16.4 },
    ];

    fixture.componentRef.setInput('sectorWeights', mockSectorWeights);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hasData()).toBe(true);

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('h3')?.textContent).toContain('Sector Exposure');
    expect(element.querySelector('canvas')).toBeTruthy();
  });

  it('renders empty state when data is empty', () => {
    fixture.componentRef.setInput('sectorWeights', []);
    fixture.detectChanges();

    const component = fixture.componentInstance;
    expect(component.hasData()).toBe(false);

    // DOM Assertions
    const element: HTMLElement = fixture.nativeElement;
    expect(element.querySelector('app-chart-empty-state')).toBeTruthy();
  });
});
