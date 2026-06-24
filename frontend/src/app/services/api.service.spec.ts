import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { mockEpisodes } from '../mock-data/travel.mock';
import { Episode } from '../models/travel.models';
import { ApiError, ApiService, UploadResult } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ApiService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(ApiService);
    httpController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpController.verify();
  });

  it('returns the episodes received from the API', () => {
    let result: Episode[] | undefined;
    service.getEpisodes().subscribe((episodes) => {
      result = episodes;
    });

    const request = httpController.expectOne('/api/episodes');
    expect(request.request.method).toBe('GET');
    request.flush(mockEpisodes);

    expect(result).toEqual(mockEpisodes);
  });

  it('returns one episode and safely encodes its identifier', () => {
    let result: Episode | undefined;
    service.getEpisode('episode/1').subscribe((episode) => {
      result = episode;
    });

    const request = httpController.expectOne('/api/episodes/episode%2F1');
    expect(request.request.method).toBe('GET');
    request.flush(mockEpisodes[0]);

    expect(result).toEqual(mockEpisodes[0]);
  });

  it('uploads files as multipart form data', () => {
    const files = [new File(['photo'], 'bali.jpg', { type: 'image/jpeg' })];
    let result: UploadResult | undefined;
    service.uploadPhotos(files).subscribe((upload) => {
      result = upload;
    });

    const request = httpController.expectOne('/api/uploads');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toBeInstanceOf(FormData);
    expect((request.request.body as FormData).get('photos')).toBeTruthy();
    request.flush({ jobId: 'job-123' });

    expect(result).toEqual({ jobId: 'job-123' });
  });

  it.each([
    { status: 400, method: 'uploadPhotos' as const, message: 'Fichier invalide' },
    { status: 404, method: 'getEpisode' as const, message: 'Episode introuvable' },
    { status: 500, method: 'getEpisodes' as const, message: 'Service indisponible' },
  ])('propagates a normalized $status API error', ({ status, method, message }) => {
    let receivedError: ApiError | undefined;

    if (method === 'uploadPhotos') {
      service.uploadPhotos([]).subscribe({ error: (error: ApiError) => (receivedError = error) });
      httpController.expectOne('/api/uploads').flush({ message }, { status, statusText: 'Error' });
    } else if (method === 'getEpisode') {
      service.getEpisode('missing').subscribe({ error: (error: ApiError) => (receivedError = error) });
      httpController.expectOne('/api/episodes/missing').flush({ message }, { status, statusText: 'Error' });
    } else {
      service.getEpisodes().subscribe({ error: (error: ApiError) => (receivedError = error) });
      httpController.expectOne('/api/episodes').flush({ message }, { status, statusText: 'Error' });
    }

    expect(receivedError).toEqual({ status, message });
  });

  it('uses a deterministic fallback message for non-JSON errors', () => {
    let receivedError: ApiError | undefined;
    service.getEpisodes().subscribe({ error: (error: ApiError) => (receivedError = error) });

    httpController.expectOne('/api/episodes').flush('failure', { status: 500, statusText: 'Error' });

    expect(receivedError).toEqual({ status: 500, message: 'Une erreur API est survenue.' });
  });
});
