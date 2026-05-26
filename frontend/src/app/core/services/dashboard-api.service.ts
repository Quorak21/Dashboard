import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class DashboardApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;
  //TODO: Gerer le soucis de any, trop vague et dangereux
  getData(path: String): Observable<any> {
    return this.http
      .get(`${this.apiUrl}/api/dashboard/${path}`)
      .pipe(catchError(() => of(null)));
  }
}
