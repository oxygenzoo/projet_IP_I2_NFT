import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, tap, throwError } from 'rxjs';

import { API_URL } from '../config/api.config';
import { DraftImage, GenerationResponse, TravelPreferences } from '../models/generation.models';

@Injectable({ providedIn: 'root' })
export class GenerationApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  readonly lastResult = signal<GenerationResponse | null>(null);

  createEpisode(images: DraftImage[], preferences: TravelPreferences, travelId?: string | null): Observable<GenerationResponse> {
    const formData = new FormData();

    for (const image of images) {
      formData.append('images', image.file, image.file.name);
    }

    formData.append('title', preferences['title'] || 'Mon voyage');
    formData.append('destination', preferences['destination'] || '');
    formData.append('preferences', JSON.stringify(preferences));
    if (travelId) {
      formData.append('travelId', travelId);
    }

    return this.http.post<GenerationResponse>(`${this.apiUrl}/api/generation/jobs`, formData).pipe(
      tap((result) => this.lastResult.set(result)),
      catchError((error: HttpErrorResponse) => {
        const message = this.extractErrorMessage(error);
        return throwError(() => new Error(message));
      }),
    );
  }

  private extractErrorMessage(error: HttpErrorResponse): string {
    const payload = error.error;

    if (typeof payload === 'string' && payload.trim()) {
      return payload;
    }

    if (payload && typeof payload === 'object') {
      const detail = 'detail' in payload ? payload.detail : undefined;
      const message = 'message' in payload ? payload.message : undefined;

      if (typeof detail === 'string' && detail) {
        return detail;
      }

      if (typeof message === 'string' && message) {
        return message;
      }
    }

    if (error.status === 0) {
      return 'API indisponible. Vérifiez que le backend et le service IA sont démarrés.';
    }

    return 'La génération IA a échoué. Réessayez dans un instant.';
  }
}
