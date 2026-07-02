import { Routes } from '@angular/router';

import { adminGuard } from './guards/admin.guard';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'admin',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./pages/admin/admin-page.component').then((component) => component.AdminPageComponent),
    title: 'Admin | NFT',
  },
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
    path: 'library',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/library/library-page.component').then((component) => component.LibraryPageComponent),
    title: 'Bibliothèque | NFT',
  },
  {
    path: 'photos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/photo-library/photo-library-page.component').then((component) => component.PhotoLibraryPageComponent),
    title: 'Photos | NFT',
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
    path: 'shared/episode/:token',
    loadComponent: () =>
      import('./pages/public-episode/public-episode-page.component').then(
        (component) => component.PublicEpisodePageComponent,
      ),
    title: 'Episode partagé | NFT',
  },
  {
    path: 'pricing',
    loadComponent: () =>
      import('./pages/pricing/pricing-page.component').then((component) => component.PricingPageComponent),
    title: 'NFT',
  },
  {
    path: 'organisations',
    loadComponent: () =>
      import('./pages/organization/organization-page.component').then((component) => component.OrganizationPageComponent),
    title: 'Organisations | NFT',
  },
  {
    path: '**',
    redirectTo: '',
  },
];
