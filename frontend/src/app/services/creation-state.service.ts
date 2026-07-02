import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Inject, Injectable, computed, effect, inject, signal } from '@angular/core';
import { Subscription, catchError, of, tap } from 'rxjs';

import { API_URL } from '../config/api.config';
import { CreationSession, CreationStatus, DraftImage, GenerationResponse, TravelPreferences } from '../models/generation.models';
import { GenerationApiService } from './generation-api.service';

interface StoredCreation {
  id?: string;
  ownerId?: string;
  travelId?: string | null;
  episodeId?: string | null;
  status: CreationStatus;
  resultVideoUrl?: string | null;
  errorMessage?: string | null;
  updatedAt: string;
  result?: GenerationResponse | null;
}

@Injectable({ providedIn: 'root' })
export class CreationStateService {
  private readonly http = inject(HttpClient);
  private readonly generationApi = inject(GenerationApiService);
  private readonly storageKey = 'nft.activeCreation';
  private generationSubscription?: Subscription;

  readonly creation = signal<StoredCreation | null>(this.readStoredCreation());
  readonly isActive = computed(() => {
    const creation = this.creation();
    return Boolean(creation && ['uploading', 'preferences', 'generating'].includes(creation.status));
  });

  constructor(@Inject(API_URL) private readonly apiUrl: string) {
    effect(() => this.persist(this.creation()));
  }

  refreshFromBackend(): void {
    this.http.get<CreationSession>(`${this.apiUrl}/api/creations/current`).pipe(
      catchError(() => of(null)),
    ).subscribe((session) => {
      if (!session) {
        return;
      }
      this.creation.set({
        id: session.id,
        ownerId: session.ownerId,
        travelId: session.travelId ?? null,
        episodeId: session.episodeId ?? null,
        status: session.status,
        resultVideoUrl: session.resultVideoUrl ?? null,
        errorMessage: session.errorMessage ?? null,
        updatedAt: session.updatedAt,
        result: this.creation()?.result ?? null,
      });
    });
  }

  startUpload(travelId?: string | null): void {
    this.setLocal('uploading', { travelId });
    this.http.post<CreationSession>(`${this.apiUrl}/api/creations`, { status: 'uploading', travelId }).pipe(
      catchError(() => of(null)),
    ).subscribe((session) => {
      if (session) {
        this.mergeBackendSession(session);
      }
    });
  }

  markPreferences(travelId?: string | null): void {
    this.update('preferences', { travelId });
  }

  beginGeneration(images: DraftImage[], preferences: TravelPreferences, travelId?: string | null): void {
    if (this.creation()?.status === 'generating' && this.generationSubscription) {
      return;
    }

    this.update('generating', { travelId });
    this.generationSubscription?.unsubscribe();
    this.generationSubscription = this.generationApi.createEpisode(images, preferences, travelId).subscribe({
      next: (result) => {
        const firstVideo = result.videos?.[0] ?? null;
        this.update('done', { resultVideoUrl: firstVideo, result });
      },
      error: (error: Error) => {
        this.update('error', { errorMessage: error.message });
      },
    });
  }

  routeForCurrent(): string[] {
    const status = this.creation()?.status;
    if (status === 'generating' || status === 'done' || status === 'error') {
      return ['/generating'];
    }
    return ['/upload'];
  }

  clear(): void {
    this.generationSubscription?.unsubscribe();
    this.generationSubscription = undefined;
    this.creation.set(null);
  }

  private update(status: CreationStatus, extra: Partial<StoredCreation> = {}): void {
    this.setLocal(status, extra);
    const creation = this.creation();
    if (!creation?.id) {
      return;
    }

    this.http.patch<CreationSession>(`${this.apiUrl}/api/creations/${creation.id}`, {
      status,
      travelId: creation.travelId,
      episodeId: creation.episodeId,
      resultVideoUrl: creation.resultVideoUrl,
      errorMessage: creation.errorMessage,
    }).pipe(
      tap((session) => this.mergeBackendSession(session)),
      catchError((error: HttpErrorResponse) => {
        console.warn('Creation status sync failed', error.message);
        return of(null);
      }),
    ).subscribe();
  }

  private setLocal(status: CreationStatus, extra: Partial<StoredCreation> = {}): void {
    this.creation.set({
      ...this.creation(),
      ...extra,
      status,
      updatedAt: new Date().toISOString(),
    });
  }

  private mergeBackendSession(session: CreationSession): void {
    this.creation.set({
      ...this.creation(),
      id: session.id,
      ownerId: session.ownerId,
      travelId: session.travelId ?? this.creation()?.travelId ?? null,
      episodeId: session.episodeId ?? this.creation()?.episodeId ?? null,
      status: session.status,
      resultVideoUrl: session.resultVideoUrl ?? this.creation()?.resultVideoUrl ?? null,
      errorMessage: session.errorMessage ?? null,
      updatedAt: session.updatedAt,
    });
  }

  private readStoredCreation(): StoredCreation | null {
    try {
      return JSON.parse(localStorage.getItem(this.storageKey) ?? 'null');
    } catch {
      return null;
    }
  }

  private persist(creation: StoredCreation | null): void {
    if (!creation) {
      localStorage.removeItem(this.storageKey);
      return;
    }
    localStorage.setItem(this.storageKey, JSON.stringify(creation));
  }
}
