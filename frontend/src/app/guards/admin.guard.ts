import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (await authService.isAdmin()) {
    return true;
  }

  return router.createUrlTree(['/home'], {
    queryParams: {
      adminDenied: '1',
    },
  });
};
