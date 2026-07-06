import { Component } from '@angular/core';
import { LucideAngularModule, Home, Zap, TrendingUp, Landmark } from 'lucide-angular';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar',
  imports: [LucideAngularModule, RouterLink, RouterLinkActive],
  templateUrl: './navbar.html',
  styleUrl: './navbar.css',
})
export class Navbar {

  readonly Home = Home;
  readonly Zap = Zap;
  readonly TrendingUp = TrendingUp;
  readonly Landmark = Landmark;

}
