import { Component, Input, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-asset-dashboard-card',
  imports: [RouterLink],
  templateUrl: './asset-dashboard-card.html',
  styleUrl: './asset-dashboard-card.css',
})
export class AssetDashboardCard implements OnInit {

  @Input() asset: string = "";

  logo: string = "";
  name: string = "";
  link: string = "";

  ngOnInit() {
    if (this.asset === "placeholder") {
      this.logo = "assets/logos/placeholder.png";
      this.name = "À venir !";
      this.link = "/dashboard";
    } else {
      this.logo = "assets/logos/" + this.asset + ".png";
      this.name = this.asset.toUpperCase();
      this.link = "/" + this.asset;
    }
  }

}
