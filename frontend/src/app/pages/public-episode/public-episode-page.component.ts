import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Episode } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-public-episode-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './public-episode-page.component.html',
})
export class PublicEpisodePageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly travelApiService = inject(TravelApiService);
  private episodeSubscription?: Subscription;

  protected readonly episode = signal<Episode | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly coverImage = computed(() => `url(${this.episode()?.coverImage ?? ''})`);

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token');

    if (!token) {
      this.errorMessage.set('Lien de partage invalide.');
      this.isLoading.set(false);
      return;
    }

    this.episodeSubscription = this.travelApiService.getPublicEpisode(token).subscribe({
      next: (episode) => {
        this.episode.set(episode);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Ce lien de partage est invalide ou expiré.');
        this.isLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.episodeSubscription?.unsubscribe();
  }
}
