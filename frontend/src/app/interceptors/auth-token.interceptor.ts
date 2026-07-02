import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, from, of, switchMap } from 'rxjs';

import { API_URL } from '../config/api.config';
import { AuthService } from '../services/auth.service';

export const authTokenInterceptor: HttpInterceptorFn = (request, next) => {
  const apiUrl = inject(API_URL);
  const normalizedApiUrl = apiUrl.replace(/\/+$/, '');

  if (!isProtectedBackendRequest(request.url, normalizedApiUrl)) {
    return next(request);
  }

  const authService = inject(AuthService);

  return from(authService.getAccessToken()).pipe(
    catchError(() => of(null)),
    switchMap((token) => {
      if (!token?.trim()) {
        return next(request);
      }

      return next(
        request.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        }),
      );
    }),
  );
};

function isProtectedBackendRequest(url: string, apiUrl: string): boolean {
  if (!apiUrl || !url.startsWith(apiUrl)) {
    return false;
  }

  const path = url.slice(apiUrl.length).replace(/^\/+/, '/');
  if (!path.startsWith('/api/')) {
    return false;
  }

  return ![
    '/api/contributions/',
    '/api/public/',
    '/api/pricing-plans',
    '/api/health',
  ].some((publicPath) => path === publicPath || path.startsWith(publicPath));
}
