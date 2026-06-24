import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing-page.component').then((component) => component.LandingPageComponent),
    title: 'NFT',
  },
  {
    path: 'home',
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
    loadComponent: () => import('./pages/upload/upload-page.component').then((component) => component.UploadPageComponent),
    title: 'NFT',
  },
  {
    path: 'preferences',
    loadComponent: () =>
      import('./pages/preferences/preferences-page.component').then((component) => component.PreferencesPageComponent),
    title: 'NFT',
  },
  {
    path: 'generating',
    loadComponent: () =>
      import('./pages/generating/generating-page.component').then((component) => component.GeneratingPageComponent),
    title: 'NFT',
  },
  {
    path: 'episode/:id',
    loadComponent: () =>
      import('./pages/episode-detail/episode-detail-page.component').then(
        (component) => component.EpisodeDetailPageComponent,
      ),
    title: 'NFT',
  },
  {
    path: 'player/:id',
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
