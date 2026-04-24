import { Component } from '@angular/core';
import { AssetDashboardCard } from '../../shared/components/asset-dashboard-card/asset-dashboard-card';

@Component({
  selector: 'app-dashboard',
  imports: [AssetDashboardCard],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class Dashboard { }
