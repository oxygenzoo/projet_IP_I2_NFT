import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Episode, Scene, Travel } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-episode-detail-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './episode-detail-page.component.html',
})
export class EpisodeDetailPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly travelApiService = inject(TravelApiService);
  private episodeSubscription?: Subscription;
  private travelSubscription?: Subscription;

  protected readonly episode = signal<Episode | null>(null);
  protected readonly travel = signal<Travel | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly coverImage = computed(() => `url(${this.episode()?.coverImage ?? ''})`);
  protected readonly sortedScenes = computed(() =>
    [...(this.episode()?.scenes ?? [])].sort(
      (left, right) => (left.order ?? 0) - (right.order ?? 0),
    ),
  );

  ngOnInit(): void {
    const episodeId = this.route.snapshot.paramMap.get('id');

    if (!episodeId) {
      this.errorMessage.set('Episode introuvable.');
      this.isLoading.set(false);
      return;
    }

    this.episodeSubscription = this.travelApiService.getEpisode(episodeId).subscribe({
      next: (episode) => {
        this.episode.set(episode);
        this.loadTravel(episode.travelId);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set("Cet episode n'existe pas dans votre espace.");
        this.isLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.episodeSubscription?.unsubscribe();
    this.travelSubscription?.unsubscribe();
  }

  private loadTravel(travelId: string): void {
    this.travelSubscription?.unsubscribe();
    this.travelSubscription = this.travelApiService.getTravel(travelId).subscribe({
      next: (travel) => this.travel.set(travel),
      error: () => this.travel.set(null),
    });
  }

  protected sceneOrder(scene: Scene, index: number): number {
    return scene.order ?? index + 1;
  }

  protected sceneVoiceOver(scene: Scene): string {
    return scene.voiceOverText ?? scene.description ?? '';
  }

  protected sceneType(scene: Scene): string {
    return scene.type ?? 'souvenir';
  }

  protected sceneStatus(scene: Scene): string {
    if (scene.isAiReconstructed) {
      return 'IA reconstruite';
    }

    return scene.generationStatus === 'generated' || !scene.generationStatus ? 'Generee' : scene.generationStatus;
  }
}
