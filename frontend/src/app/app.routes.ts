import { Routes } from '@angular/router';

import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing-page.component').then((component) => component.LandingPageComponent),
    title: 'NFT',
  },
  {
    path: 'home',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/home/home-page.component').then((component) => component.HomePageComponent),
    title: 'NFT',
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login-page.component').then((component) => component.LoginPageComponent),
    title: 'Connexion | NFT',
  },
  {
    path: 'upload',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/upload/upload-page.component').then((component) => component.UploadPageComponent),
    title: 'NFT',
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/profile/profile-page.component').then((component) => component.ProfilePageComponent),
    title: 'Profil | NFT',
  },
  {
    path: 'notifications',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/notifications/notifications-page.component').then(
        (component) => component.NotificationsPageComponent,
      ),
    title: 'Notifications | NFT',
  },
  {
    path: 'preferences/:travelId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/preferences/preferences-page.component').then((component) => component.PreferencesPageComponent),
    title: 'NFT',
  },
  {
    path: 'preferences',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/preferences/preferences-page.component').then((component) => component.PreferencesPageComponent),
    title: 'NFT',
  },
  {
    path: 'generating',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/generating/generating-page.component').then((component) => component.GeneratingPageComponent),
    title: 'NFT',
  },
  {
    path: 'episode/:id',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/episode-detail/episode-detail-page.component').then(
        (component) => component.EpisodeDetailPageComponent,
      ),
    title: 'NFT',
  },
  {
    path: 'player/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/player/player-page.component').then((component) => component.PlayerPageComponent),
    title: 'NFT',
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pages/pricing/pricing-page.component').then((component) => component.PricingPageComponent),
    title: 'NFT',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
