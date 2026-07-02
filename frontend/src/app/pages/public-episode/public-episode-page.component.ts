import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { Episode } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-public-episode-page',
  imports: [AppLogoComponent],
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
  protected readonly sharedByLabel = computed(() => {
    const sharedBy = this.episode()?.sharedBy?.trim();
    return `Souvenir partagé par ${sharedBy || 'un voyageur'}`;
  });

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

  protected summary(episode: Episode): string {
    return episode.summary || episode.introText || 'Un souvenir de voyage à regarder et partager.';
  }

  protected metadataItems(episode: Episode): string[] {
    return [
      episode.location || episode.locationName || '',
      episode.duration || '',
      episode.photoCount > 0 ? `${episode.photoCount} photos` : '',
    ].filter((item): item is string => Boolean(item.trim()));
  }
}
