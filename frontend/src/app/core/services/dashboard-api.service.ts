import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import type { HypeDto, InveBDto } from '../models';
import { ToastService } from './toastService';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly toastService = inject(ToastService);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getData(path: 'hype'): Observable<HypeDto | null>;
  getData(path: 'inveb'): Observable<InveBDto | null>;
  getData(path: string): Observable<HypeDto | InveBDto | null> {
    return this.http.get<HypeDto | InveBDto>(`${this.apiUrl}/api/dashboard/${path}`).pipe(
      catchError(() => {
        this.toastService.showError(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard',
        );
        return of(null);
      }),
    );
  }
}
