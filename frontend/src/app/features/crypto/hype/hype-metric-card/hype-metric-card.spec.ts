import { ComponentFixture, TestBed } from '@angular/core/testing';

import { HypeMetricCard } from './hype-metric-card';

describe('HypeMetricCard', () => {
  let component: HypeMetricCard;
  let fixture: ComponentFixture<HypeMetricCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HypeMetricCard],
    }).compileComponents();

    fixture = TestBed.createComponent(HypeMetricCard);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('card', {
      title: 'Circulating Supply',
      metrics: [
        { label: 'Circulating', value: '123,456' },
        { label: 'APR', value: '12.34%', variation: 1.23, ratio: '2x' }
      ]
    });
    await fixture.whenStable();
  });

  it('renders title and metric values from input card', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('Circulating Supply');
    expect(text).toContain('Circulating');
    expect(text).toContain('123,456');
    expect(text).toContain('APR');
    expect(text).toContain('+1.23%');
    expect(text).toContain('2x');
  });
});
