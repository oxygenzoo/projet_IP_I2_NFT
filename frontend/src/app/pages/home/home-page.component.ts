import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Episode, Travel } from '../../models/travel.models';
import { AuthService, ConnectedProfile } from '../../services/auth.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';
import { EpisodeCardComponent } from '../../shared/episode-card/episode-card.component';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, AppLogoComponent, EpisodeCardComponent],
  templateUrl: './home-page.component.html',
})
export class HomePageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly travelApiService = inject(TravelApiService);

  protected readonly profile = signal<ConnectedProfile | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly travels = signal<Travel[]>([]);
  protected readonly episodes = signal<Episode[]>([]);
  protected readonly featured = computed(() => this.travels().find((travel) => travel.featured) ?? this.travels()[0] ?? null);
  protected readonly featuredEpisode = computed(() => this.featured()?.episodes[0] ?? null);
  protected readonly recentEpisodes = computed(() => this.episodes().slice(0, 6));
  protected readonly recommendedTravels = computed(() => this.travels().filter((travel) => !travel.featured));
  protected readonly heroImage = computed(() => `url(${this.featured()?.heroImage ?? ''})`);
  protected readonly profileName = computed(() => this.profile()?.name ?? 'Voyageur');
  protected readonly profileInitials = computed(() => this.profile()?.initials ?? 'NF');
  protected readonly profileAvatarUrl = computed(() => this.profile()?.avatarUrl ?? '');
  protected readonly totalPhotos = computed(() => this.travels().reduce((total, travel) => total + travel.photoCount, 0));
  protected readonly totalEpisodes = computed(() => this.episodes().length);

  ngOnInit(): void {
    void this.loadProfile();
    this.loadDashboard();
  }

  private async loadProfile(): Promise<void> {
    this.profile.set(await this.authService.getCurrentProfile());
  }

  protected loadDashboard(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      travels: this.travelApiService.getTravels(),
      episodes: this.travelApiService.getEpisodes(),
    }).subscribe({
      next: ({ travels, episodes }) => {
        this.travels.set(travels);
        this.episodes.set(episodes);
        this.isLoading.set(false);
      },
      error: () => {
        this.travels.set([]);
        this.episodes.set([]);
        this.errorMessage.set("Impossible de charger les voyages depuis l'API. Vérifiez que le backend Render est démarré.");
        this.isLoading.set(false);
      },
    });
  }

  protected travelPlayerLink(travel: Travel): string[] {
    const firstEpisode = travel.episodes[0];
    return firstEpisode ? ['/episode', firstEpisode.id] : ['/home'];
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
    await this.router.navigate(['/login']);
  }
}
