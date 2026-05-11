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
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
