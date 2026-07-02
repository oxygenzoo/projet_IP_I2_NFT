import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { Episode, Photo, PricingPlan, Scene, Travel } from '../models/travel.models';

export interface SaveTravelPayload {
  userId?: string | null;
  title: string;
  destination?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  description?: string | null;
}

export interface ContributionLink {
  id: string;
  travelId: string;
  token: string;
  url: string;
  expiresAt: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class TravelApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_URL) private readonly apiUrl: string,
  ) {}

  getTravels(accessToken?: string): Observable<Travel[]> {
    return this.http.get<Travel[]>(`${this.apiUrl}/api/travels`, this.authOptions(accessToken));
  }

  getTravel(id: string, accessToken?: string): Observable<Travel> {
    return this.http.get<Travel>(`${this.apiUrl}/api/travels/${id}`, this.authOptions(accessToken));
  }

  createTravel(payload: SaveTravelPayload, accessToken?: string): Observable<Travel> {
    return this.http.post<Travel>(`${this.apiUrl}/api/travels`, payload, this.authOptions(accessToken));
  }

  getEpisodes(): Observable<Episode[]> {
    return this.http.get<Episode[]>(`${this.apiUrl}/api/episodes`);
  }

  getEpisode(id: string): Observable<Episode> {
    return this.http.get<Episode>(`${this.apiUrl}/api/episodes/${id}`);
  }

  getPublicEpisode(shareToken: string): Observable<Episode> {
    return this.http.get<Episode>(`${this.apiUrl}/api/public/episodes/${shareToken}`);
  }

  getPricingPlans(): Observable<PricingPlan[]> {
    return this.http.get<PricingPlan[]>(`${this.apiUrl}/api/pricing-plans`);
  }

  getPhotos(travelId: string): Observable<Photo[]> {
    return this.http.get<Photo[]>(`${this.apiUrl}/api/travels/${travelId}/photos`);
  }

  uploadPhoto(travelId: string, file: File, consentRgpd = true): Observable<Photo> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('consentRgpd', String(consentRgpd));
    return this.http.post<Photo>(`${this.apiUrl}/api/travels/${travelId}/photos`, formData);
  }

  createContributionLink(travelId: string): Observable<ContributionLink> {
    return this.http.post<ContributionLink>(`${this.apiUrl}/api/travels/${travelId}/contribution-links`, {});
  }

  getContributionInfo(token: string): Observable<{ travelId: string; title: string; expiresAt: string }> {
    return this.http.get<{ travelId: string; title: string; expiresAt: string }>(`${this.apiUrl}/api/contributions/${token}`);
  }

  uploadContributionPhoto(token: string, file: File, consentRgpd = true): Observable<Photo> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append('consentRgpd', String(consentRgpd));
    return this.http.post<Photo>(`${this.apiUrl}/api/contributions/${token}/photos`, formData);
  }

  deletePhoto(travelId: string, photoId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/travels/${travelId}/photos/${photoId}`);
  }

  shareEpisode(travelId: string, episodeId: string): Observable<Episode> {
    return this.http.post<Episode>(`${this.apiUrl}/api/travels/${travelId}/episodes/${episodeId}/share`, {});
  }

  exportEpisode(travelId: string, episodeId: string, fail = false): Observable<Episode> {
    return this.http.post<Episode>(
      `${this.apiUrl}/api/travels/${travelId}/episodes/${episodeId}/export`,
      {},
      { params: { fail } },
    );
  }

  updateEpisodeFavorite(travelId: string, episodeId: string, favorite: boolean): Observable<Episode> {
    return this.http.patch<Episode>(
      `${this.apiUrl}/api/travels/${travelId}/episodes/${episodeId}/favorite`,
      {},
      { params: { favorite } },
    );
  }

  deleteEpisode(travelId: string, episodeId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/api/travels/${travelId}/episodes/${episodeId}`);
  }

  private authOptions(accessToken?: string): { headers?: HttpHeaders } {
    const token = accessToken?.trim();
    return token
      ? { headers: new HttpHeaders({ Authorization: `Bearer ${token}` }) }
      : {};
  }
}
