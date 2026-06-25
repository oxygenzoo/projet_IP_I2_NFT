import { isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Episode, Travel } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-player-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './player-page.component.html',
})
export class PlayerPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly travelApiService = inject(TravelApiService);
  private readonly platformId = inject(PLATFORM_ID);
  private intervalId?: ReturnType<typeof setInterval>;
  private episodeSubscription?: Subscription;
  private travelSubscription?: Subscription;

  protected readonly episode = signal<Episode | null>(null);
  protected readonly travel = signal<Travel | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly playing = signal(true);
  protected readonly progress = signal(0);
  protected readonly stillImage = computed(() => `url(${this.episode()?.videoStill ?? ''})`);

  ngOnInit(): void {
    this.loadEpisode();

    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    this.intervalId = setInterval(() => {
      if (!this.playing()) {
        return;
      }

      this.progress.update((current) => Math.min(current + 1.4, 100));
      if (this.progress() >= 100) {
        this.playing.set(false);
      }
    }, 900);
  }

  ngOnDestroy(): void {
    this.episodeSubscription?.unsubscribe();
    this.travelSubscription?.unsubscribe();

    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = undefined;
    }
  }

  protected togglePlayback(): void {
    if (this.progress() >= 100) {
      this.progress.set(0);
    }

    this.playing.update((current) => !current);
  }

  private loadEpisode(): void {
    const episodeId = this.route.snapshot.paramMap.get('id');

    if (!episodeId) {
      this.errorMessage.set('Episode introuvable.');
      this.isLoading.set(false);
      return;
    }

    this.episodeSubscription = this.travelApiService.getEpisode(episodeId).subscribe({
      next: (episode) => {
        this.episode.set(episode);
        this.progress.set(episode.progress > 0 && episode.progress < 100 ? episode.progress : 0);
        this.loadTravel(episode.travelId);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set("Cet episode n'existe pas dans votre espace.");
        this.playing.set(false);
        this.isLoading.set(false);
      },
    });
  }

  private loadTravel(travelId: string): void {
    this.travelSubscription?.unsubscribe();
    this.travelSubscription = this.travelApiService.getTravel(travelId).subscribe({
      next: (travel) => this.travel.set(travel),
      error: () => this.travel.set(null),
    });
  }
}
