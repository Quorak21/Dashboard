import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Background } from './background';

describe('Background', () => {
  let component: Background;
  let fixture: ComponentFixture<Background>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Background],
    }).compileComponents();

    fixture = TestBed.createComponent(Background);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('renders decorative background image with expected source', () => {
    fixture.detectChanges();
    const image = fixture.nativeElement.querySelector('img') as HTMLImageElement | null;

    expect(image).toBeTruthy();
    expect(image?.getAttribute('src')).toBe('assets/images/background.png');
    expect(image?.getAttribute('alt')).toContain('background');
  });
});
