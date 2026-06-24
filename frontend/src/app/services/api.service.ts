import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, Observable, throwError } from 'rxjs';
import { Episode } from '../models/travel.models';

export interface UploadResult {
  jobId: string;
}

export interface ApiError {
  status: number;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api';

  getEpisodes(): Observable<Episode[]> {
    return this.http
      .get<Episode[]>(`${this.apiUrl}/episodes`)
      .pipe(catchError((error: HttpErrorResponse) => this.handleError(error)));
  }

  getEpisode(id: string): Observable<Episode> {
    return this.http
      .get<Episode>(`${this.apiUrl}/episodes/${encodeURIComponent(id)}`)
      .pipe(catchError((error: HttpErrorResponse) => this.handleError(error)));
  }

  uploadPhotos(files: File[]): Observable<UploadResult> {
    const body = new FormData();
    files.forEach((file) => body.append('photos', file, file.name));

    return this.http
      .post<UploadResult>(`${this.apiUrl}/uploads`, body)
      .pipe(catchError((error: HttpErrorResponse) => this.handleError(error)));
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    const apiMessage =
      typeof error.error === 'object' && error.error?.message
        ? String(error.error.message)
        : 'Une erreur API est survenue.';

    return throwError(() => ({ status: error.status, message: apiMessage }) satisfies ApiError);
  }
}
