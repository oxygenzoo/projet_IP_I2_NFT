import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Episode, Scene, Travel } from '../models/travel.models';

@Injectable({
  providedIn: 'root',
})
export class TravelApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_URL) private readonly apiUrl: string,
  ) {}

  getTravels(): Observable<Travel[]> {
    return this.http.get<Travel[]>(`${this.apiUrl}/api/travels`);
  }

  getTravel(id: string): Observable<Travel> {
    return this.http.get<Travel>(`${this.apiUrl}/api/travels/${id}`);
  }

  getEpisodes(): Observable<Episode[]> {
    return this.http.get<Episode[]>(`${this.apiUrl}/api/episodes`);
  }

  getEpisode(id: string): Observable<Episode> {
    return this.http.get<Episode>(`${this.apiUrl}/api/episodes/${id}`);
  }

  getEpisodeScenes(episodeId: string): Observable<Scene[]> {
    return this.http.get<Scene[]>(`${this.apiUrl}/api/episodes/${episodeId}/scenes`);
  }
}
