import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './core/components/navbar/navbar';
import { Footer } from './core/components/footer/footer';
import { Background } from './shared/components/background/background';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, Footer, Background],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('Dashboard');
}
