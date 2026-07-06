import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { HypeDto, AssetDto, QuarterlyAlertsResponse, RegisteredAssetDto } from '../models';
import { ToastService } from './toastService';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly toastService = inject(ToastService);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getData(path: 'hype'): Observable<HypeDto | null> {
    return this.http.get<HypeDto>(`${this.apiUrl}/api/dashboard/${path}`).pipe(
      catchError(() => {
        this.toastService.showError(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard',
        );
        return of(null);
      }),
    );
  }

  getAsset(assetId: string): Observable<AssetDto | null> {
    if (!assetId || typeof assetId !== 'string' || assetId.trim() === '' || assetId.includes('/')) {
      return of(null);
    }
    const cleanId = encodeURIComponent(assetId.trim());
    return this.http.get<AssetDto>(`${this.apiUrl}/api/dashboard/asset/${cleanId}`).pipe(
      catchError(() => {
        this.toastService.showError(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard',
        );
        return of(null);
      }),
    );
  }

  getQuarterlyAlerts(): Observable<QuarterlyAlertsResponse | null> {
    return this.http.get<QuarterlyAlertsResponse>(`${this.apiUrl}/api/dashboard/alerts/quarterly`).pipe(
      catchError(() => {
        this.toastService.showError(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard',
        );
        return of(null);
      }),
    );
  }

  getRegisteredAssets(): Observable<RegisteredAssetDto[] | null> {
    return this.http.get<RegisteredAssetDto[]>(`${this.apiUrl}/api/dashboard/assets`).pipe(
      catchError(() => {
        this.toastService.showError(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard',
        );
        return of(null);
      }),
    );
  }
}
