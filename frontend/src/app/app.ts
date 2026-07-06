import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Navbar } from './core/components/navbar/navbar';
import { Footer } from './core/components/footer/footer';
import { Background } from './shared/components/background/background';
import { ToastComponent } from './shared/components/toast/toast';
import { QuarterlyAlertsBanner } from './shared/components/quarterly-alerts-banner/quarterly-alerts-banner';


@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Navbar, Footer, Background, ToastComponent, QuarterlyAlertsBanner],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}