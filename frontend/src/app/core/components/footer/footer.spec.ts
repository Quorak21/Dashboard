import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Footer } from './footer';

describe('Footer', () => {
  let component: Footer;
  let fixture: ComponentFixture<Footer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Footer],
    }).compileComponents();

    fixture = TestBed.createComponent(Footer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('exposes current year and renders copyright text', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent as string;
    const currentYear = new Date().getFullYear();

    expect(component.currentYear).toBe(currentYear);
    expect(text).toContain(`© ${currentYear} Dokk Corp.`);
  });
});
