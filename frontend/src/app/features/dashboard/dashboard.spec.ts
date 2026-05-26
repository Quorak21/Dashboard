import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { Dashboard } from './dashboard';
import { DashboardApiService } from '../../core/services/dashboard-api.service';

describe('Mon Premier Test Dashboard', () => {

  // 1. On fabrique notre téléphone jouet (le mock)
  const fauxServiceApi = {
    getData: (cle: string) => {
      // Si le composant demande 'hype', on lui répond direct 1.25 $ avec +5.42% de hausse
      if (cle === 'hype') {
        return of({ currentPrice: 1.25, priceChangePercentage24h: 5.42 });
      }
      // Si le composant demande 'inveb', on lui répond direct 245.50 SEK avec -1.25% de baisse
      if (cle === 'inveb') {
        return of({ currentPrice: 245.50, priceChangePercentage24h: -1.25 });
      }
      return of(null);
    }
  };

  it('devrait bien recevoir et stocker les prix de Hype et Inveb', () => {
    // 2. On prépare la scène de théâtre
    TestBed.configureTestingModule({
      imports: [Dashboard],
      providers: [
        { provide: DashboardApiService, useValue: fauxServiceApi }, // On donne le jouet
        provideRouter([]) // Requis pour éviter les erreurs de liens
      ]
    });

    // 3. On fait entrer l'acteur sur scène
    const fixture = TestBed.createComponent(Dashboard);
    const composant = fixture.componentInstance;

    // 4. "Action !" (On force Angular à charger les données)
    fixture.detectChanges();

    // 5. On vérifie si les variables de prix contiennent bien nos valeurs simulées
    expect(composant.hypePrice()).toBe(1.25);
    expect(composant.invebPrice()).toBe(245.50);

    // 6. On vérifie également les pourcentages de variation
    expect(composant.hypeChange()).toBe(5.42);
    expect(composant.invebChange()).toBe(-1.25);
  });
});
