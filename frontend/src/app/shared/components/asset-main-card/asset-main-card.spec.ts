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
    fixture.componentRef.setInput('asset', 'HYPE');
    fixture.componentRef.setInput('symbol', 'HYPE');
    fixture.componentRef.setInput('tag', 'crypto');
    fixture.componentRef.setInput('actualPrice', 12.5);
    fixture.componentRef.setInput('change24h', 3.21);
    fixture.componentRef.setInput('marketCap', 1234567);
    fixture.componentRef.setInput('volume24h', 765432);
    fixture.componentRef.setInput('lastRefresh', 1711111111111);
    fixture.componentRef.setInput('currencySymbol', '$');
    fixture.componentRef.setInput('marketClosed', true);
    await fixture.whenStable();
  });

  it('renders core asset information and closed market badge', () => {
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;

    expect(text).toContain('HYPE');
    expect(text).toContain('crypto');
    expect(text).toContain('Fermé');
    expect(text).toContain('+3.21%');
    expect(text).toContain('Cap:');
    expect(text).toContain('Vol 24h:');
  });
});
