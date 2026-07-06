import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { DashboardApiService } from './dashboard-api.service';
import { ToastService } from './toastService';
import { environment } from '../../../environments/environment';
import type { AssetDto, QuarterlyAlertsResponse } from '../models';

describe('DashboardApiService', () => {
  let service: DashboardApiService;
  let httpMock: HttpTestingController;
  let toastServiceSpy: { showError: ReturnType<typeof vi.fn> };
  const apiUrl = environment.apiUrl;

  beforeEach(() => {
    toastServiceSpy = {
      showError: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        DashboardApiService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ToastService, useValue: toastServiceSpy }
      ]
    });

    service = TestBed.inject(DashboardApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getAsset', () => {
    const mockAssetId = 'inveb';
    const mockAssetData: Partial<AssetDto> = {
      assetId: 'inveb',
      symbol: 'INVE-B',
      displayName: 'Investor AB'
    };

    it('should retrieve asset data successfully', () => {
      let emitted = false;
      service.getAsset(mockAssetId).subscribe((data) => {
        expect(data).toEqual(mockAssetData as AssetDto);
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/asset/inveb`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAssetData);
      expect(emitted).toBe(true);
    });

    it('should return null and show error toast on failure', () => {
      let emitted = false;
      service.getAsset(mockAssetId).subscribe((data) => {
        expect(data).toBeNull();
        expect(toastServiceSpy.showError).toHaveBeenCalledWith(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard'
        );
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/asset/inveb`);
      expect(req.request.method).toBe('GET');
      req.flush('Error', { status: 500, statusText: 'Server Error' });
      expect(emitted).toBe(true);
    });

    it('should return null and not make http request if assetId is invalid', () => {
      let emittedCount = 0;
      service.getAsset('').subscribe((data) => {
        expect(data).toBeNull();
        emittedCount++;
      });
      service.getAsset('   ').subscribe((data) => {
        expect(data).toBeNull();
        emittedCount++;
      });
      service.getAsset('inveb/other').subscribe((data) => {
        expect(data).toBeNull();
        emittedCount++;
      });

      httpMock.expectNone(`${apiUrl}/api/dashboard/asset/`);
      httpMock.expectNone(`${apiUrl}/api/dashboard/asset/inveb/other`);
      expect(emittedCount).toBe(3);
    });

    it('should trim and url-encode assetId', () => {
      let emitted = false;
      service.getAsset('  inve b  ').subscribe((data) => {
        expect(data).toEqual(mockAssetData as AssetDto);
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/asset/inve%20b`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAssetData);
      expect(emitted).toBe(true);
    });
  });

  describe('getQuarterlyAlerts', () => {
    const mockAlertsResponse: QuarterlyAlertsResponse = {
      alerts: [
        {
          assetId: 'inveb',
          displayName: 'Investor AB',
          label: 'Investor AB',
          updatedAt: '2026-06-30',
          daysStale: 12
        }
      ]
    };

    it('should retrieve quarterly alerts successfully', () => {
      let emitted = false;
      service.getQuarterlyAlerts().subscribe((data) => {
        expect(data).toEqual(mockAlertsResponse);
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/alerts/quarterly`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAlertsResponse);
      expect(emitted).toBe(true);
    });

    it('should return null and show error toast on failure', () => {
      let emitted = false;
      service.getQuarterlyAlerts().subscribe((data) => {
        expect(data).toBeNull();
        expect(toastServiceSpy.showError).toHaveBeenCalledWith(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard'
        );
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/alerts/quarterly`);
      expect(req.request.method).toBe('GET');
      req.flush('Error', { status: 500, statusText: 'Server Error' });
      expect(emitted).toBe(true);
    });
  });

  describe('getRegisteredAssets', () => {
    const mockAssets = [
      { id: 'inveb', displayName: 'Investor AB', type: 'STOCK', currency: 'SEK' }
    ];

    it('should retrieve registered assets successfully', () => {
      let emitted = false;
      service.getRegisteredAssets().subscribe((data) => {
        expect(data).toEqual(mockAssets);
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/assets`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAssets);
      expect(emitted).toBe(true);
    });

    it('should return null and show error toast on failure', () => {
      let emitted = false;
      service.getRegisteredAssets().subscribe((data) => {
        expect(data).toBeNull();
        expect(toastServiceSpy.showError).toHaveBeenCalledWith(
          'Erreur de connexion avec le serveur, veuillez réessayer plus tard'
        );
        emitted = true;
      });

      const req = httpMock.expectOne(`${apiUrl}/api/dashboard/assets`);
      expect(req.request.method).toBe('GET');
      req.flush('Error', { status: 500, statusText: 'Server Error' });
      expect(emitted).toBe(true);
    });
  });
});
