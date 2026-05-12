import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Inveb } from './inveb';

describe('Inveb', () => {
  let component: Inveb;
  let fixture: ComponentFixture<Inveb>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Inveb],
    }).compileComponents();

    fixture = TestBed.createComponent(Inveb);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
