import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CurrencyService {
  wantChf = signal<boolean>(false);

  toggleChf() {
    this.wantChf.set(!this.wantChf());
  }
}
