import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { TravelPreferenceResponse, TravelPreferences } from '../models/generation.models';

@Injectable({ providedIn: 'root' })
export class PreferenceApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = inject(API_URL);

  getPreferences(travelId: string): Observable<TravelPreferenceResponse> {
    return this.http.get<TravelPreferenceResponse>(`${this.apiUrl}/api/travels/${travelId}/preferences`);
  }

  savePreferences(travelId: string, preferences: TravelPreferences): Observable<TravelPreferenceResponse> {
    return this.http.post<TravelPreferenceResponse>(`${this.apiUrl}/api/travels/${travelId}/preferences`, preferences);
  }
}
