import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { Episode, Travel } from '../../models/travel.models';
import { AuthService } from '../../services/auth.service';
import { MockTravelService } from '../../services/mock-travel.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './home-page.component.html',
})
export class HomePageComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly travelService = inject(MockTravelService);
  private readonly travelApiService = inject(TravelApiService);

  protected readonly user = this.travelService.getUser();
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly travels = signal<Travel[]>([]);
  protected readonly episodes = signal<Episode[]>([]);
  protected readonly featured = computed(() => this.travels().find((travel) => travel.featured) ?? this.travels()[0] ?? null);
  protected readonly featuredEpisode = computed(() => this.featured()?.episodes[0] ?? this.episodes()[0] ?? null);
  protected readonly recentEpisodes = computed(() => this.episodes().slice(0, 6));
  protected readonly recommendedTravels = computed(() => this.travels().filter((travel) => !travel.featured));
  protected readonly heroImage = computed(() => `url(${this.featured()?.heroImage ?? ''})`);

  ngOnInit(): void {
    this.loadDashboard();
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
    return ['/episode', travel.episodes[0]?.id ?? '1'];
  }

  protected episodePlayerLink(episode: Episode): string[] {
    return ['/player', episode.id];
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
    await this.router.navigate(['/login']);
  }
}
