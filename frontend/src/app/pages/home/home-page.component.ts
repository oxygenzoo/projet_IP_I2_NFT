import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { Episode, Travel } from '../../models/travel.models';
import { AuthService, ConnectedProfile } from '../../services/auth.service';
import { CreationStateService } from '../../services/creation-state.service';
import { I18nService } from '../../services/i18n.service';
import { ContributionLink, TravelApiService } from '../../services/travel-api.service';
import { UserProfileApiService } from '../../services/user-profile-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';
import { EpisodeCardComponent } from '../../shared/episode-card/episode-card.component';

type RenderServiceState = 'pending' | 'ready' | 'contacted' | 'error';

type RenderServiceStatus = {
  name: string;
  url: string;
  state: RenderServiceState;
};

const RENDER_SERVICES: RenderServiceStatus[] = [
  {
    name: 'Service IA',
    url: 'https://projet-ip-i2-nft-1.onrender.com/health',
    state: 'pending',
  },
  {
    name: 'Backend',
    url: 'https://projet-ip-i2-nft.onrender.com/health',
    state: 'pending',
  },
];

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, AppLogoComponent, EpisodeCardComponent],
  templateUrl: './home-page.component.html',
})
export class HomePageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  protected readonly creationState = inject(CreationStateService);
  private readonly i18n = inject(I18nService);
  private readonly router = inject(Router);
  private readonly travelApiService = inject(TravelApiService);
  private readonly userProfileApi = inject(UserProfileApiService);

  protected readonly profile = signal<ConnectedProfile | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly notificationCount = signal(0);
  protected readonly renderServices = signal<RenderServiceStatus[]>(RENDER_SERVICES);
  protected readonly travels = signal<Travel[]>([]);
  protected readonly episodes = signal<Episode[]>([]);
  protected readonly featured = computed(() => this.travels().find((travel) => travel.featured) ?? this.travels()[0] ?? null);
  protected readonly featuredEpisode = computed(() => this.featured()?.episodes[0] ?? null);
  protected readonly recentEpisodes = computed(() => this.episodes().slice(0, 6));
  protected readonly favoriteEpisodes = computed(() => this.episodes().filter((episode) => episode.favorite).slice(0, 6));
  protected readonly heroImage = computed(() => `url(${this.featured()?.heroImage ?? ''})`);
  protected readonly profileName = computed(() => this.profile()?.name ?? 'Voyageur');
  protected readonly profileInitials = computed(() => this.profile()?.initials ?? 'NF');
  protected readonly profileAvatarUrl = computed(() => this.profile()?.avatarUrl ?? '');
  protected readonly totalPhotos = computed(() => this.travels().reduce((total, travel) => total + travel.photoCount, 0));
  protected readonly totalEpisodes = computed(() => this.episodes().length);
  protected readonly createButtonLabel = computed(() =>
    this.creationState.isActive() ? this.i18n.t('creationInProgress') : this.i18n.t('createMemory'),
  );
  protected readonly createButtonLink = computed(() => this.creationState.routeForCurrent());

  ngOnInit(): void {
    this.creationState.refreshFromBackend();
    setTimeout(() => this.handleFinishedCreation(), 1600);
    void this.loadProfile();
    this.loadDashboard();
  }

  private async loadProfile(): Promise<void> {
    const profile = await this.authService.getCurrentProfile();
    if (!profile) {
      return;
    }

    this.userProfileApi.getMe().pipe(catchError(() => of(null))).subscribe((backendProfile) => {
      const language = backendProfile?.language ?? profile.language;
      this.i18n.setLanguage(language);
      this.profile.set({ ...profile, language });
    });
  }

  protected loadDashboard(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    void this.wakeRenderServices();

    forkJoin({
      travels: this.travelApiService.getTravels().pipe(catchError(() => of([]))),
      episodes: this.travelApiService.getEpisodes().pipe(catchError(() => of([]))),
      contributionLinks: this.travelApiService.getContributionLinks().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ travels, episodes, contributionLinks }) => {
        this.travels.set(travels);
        this.episodes.set(episodes);
        this.notificationCount.set(this.countNotifications(episodes, contributionLinks));
        this.handleFinishedCreation();
        this.isLoading.set(false);
      },
      error: () => {
        this.travels.set([]);
        this.episodes.set([]);
        this.errorMessage.set('');
        this.isLoading.set(false);
      },
    });
  }

  protected travelPlayerLink(travel: Travel): string[] {
    const firstEpisode = travel.episodes[0];
    return firstEpisode ? ['/episode', firstEpisode.id] : ['/home'];
  }

  protected renderStatusLabel(state: RenderServiceState): string {
    if (state === 'ready') {
      return 'Connecté';
    }

    if (state === 'contacted') {
      return 'Réveil envoyé';
    }

    if (state === 'error') {
      return 'À relancer';
    }

    return 'Réveil en cours';
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
    await this.router.navigate(['/login']);
  }

  protected createButtonClasses(): Record<string, boolean> {
    return {
      'floating-create--loading': this.creationState.isActive(),
    };
  }

  private handleFinishedCreation(): void {
    const creation = this.creationState.creation();
    if (creation?.status !== 'done') {
      return;
    }

    this.notificationCount.update((count) => Math.max(count, 1));
    this.creationState.acknowledgeFinished();
  }

  private countNotifications(episodes: Episode[], contributionLinks: ContributionLink[]): number {
    const readIds = this.readNotificationIds();
    const ids = [
      ...episodes
        .filter((episode) => episode.videoUrl || ['completed', 'ready'].includes((episode.status ?? '').toLowerCase()))
        .map((episode) => `episode-ready-${episode.id}`),
      ...contributionLinks.flatMap((link) => [
        link.openedAt ? `contribution-opened-${link.id}` : '',
        (link.uploadCount ?? 0) > 0 ? `contribution-uploaded-${link.id}-${link.uploadCount}` : '',
      ]),
    ].filter(Boolean);

    return ids.filter((id) => !readIds.includes(id)).length;
  }

  private readNotificationIds(): string[] {
    try {
      return JSON.parse(localStorage.getItem('nft.readNotifications') ?? '[]');
    } catch {
      return [];
    }
  }

  private async wakeRenderServices(): Promise<void> {
    this.renderServices.set(RENDER_SERVICES.map((service) => ({ ...service, state: 'pending' })));

    await Promise.all(
      RENDER_SERVICES.map(async (service) => {
        const state = await this.pingRenderService(service.url);
        this.renderServices.update((services) =>
          services.map((current) => (current.url === service.url ? { ...current, state } : current)),
        );
      }),
    );
  }

  private async pingRenderService(url: string): Promise<RenderServiceState> {
    try {
      const response = await fetch(url, { cache: 'no-store' });
      return response.ok ? 'ready' : 'contacted';
    } catch {
      try {
        await fetch(url, { cache: 'no-store', mode: 'no-cors' });
        return 'contacted';
      } catch {
        return 'error';
      }
    }
  }
}
