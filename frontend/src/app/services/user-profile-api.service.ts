import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_URL } from '../config/api.config';
import { AppLanguage } from './i18n.service';

export interface BackendUserProfile {
  id: string;
  email: string;
  language: AppLanguage;
}

@Injectable({ providedIn: 'root' })
export class UserProfileApiService {
  constructor(
    private readonly http: HttpClient,
    @Inject(API_URL) private readonly apiUrl: string,
  ) {}

  updateLanguage(language: AppLanguage): Observable<BackendUserProfile> {
    return this.http.patch<BackendUserProfile>(`${this.apiUrl}/api/users/me/language`, { language });
  }
}
