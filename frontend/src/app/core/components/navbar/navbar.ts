import { Component, inject } from '@angular/core';
import { LucideAngularModule, Home, Menu } from 'lucide-angular';
import { RouterLink } from '@angular/router';
import { CurrencyService } from '../../services/currency.service';

@Component({
  selector: 'app-navbar',
  imports: [LucideAngularModule, RouterLink],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {

  readonly Home = Home;
  readonly Menu = Menu;
  public currencyService = inject(CurrencyService);

}
