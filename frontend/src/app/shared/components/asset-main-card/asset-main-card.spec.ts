import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AssetMainCard } from './asset-main-card';

describe('AssetMainCard', () => {
  let component: AssetMainCard;
  let fixture: ComponentFixture<AssetMainCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetMainCard],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetMainCard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
