import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { Episode, Scene, SceneGenerationStatus, Travel } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

const STATUS_LABELS: Record<SceneGenerationStatus, string> = {
  pending: 'En attente',
  generating: 'En génération',
  completed: 'Générée',
  failed: 'Échec',
};

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
  private shareSubscription?: Subscription;
  private exportSubscription?: Subscription;

  protected readonly episode = signal<Episode | null>(null);
  protected readonly travel = signal<Travel | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly shareMessage = signal('');
  protected readonly exportMessage = signal('');
  protected readonly isExporting = signal(false);
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
    this.shareSubscription?.unsubscribe();
    this.exportSubscription?.unsubscribe();
  }

  protected shareEpisode(): void {
    const episode = this.episode();

    if (!episode) {
      return;
    }

    this.shareMessage.set('');
    this.shareSubscription?.unsubscribe();
    this.shareSubscription = this.travelApiService.shareEpisode(episode.travelId, episode.id).subscribe({
      next: (sharedEpisode) => {
        this.episode.set({ ...episode, ...sharedEpisode });
        this.copyShareLink(sharedEpisode.shareToken);
      },
      error: () => this.shareMessage.set('Partage impossible pour le moment.'),
    });
  }

  protected exportEpisode(): void {
    const episode = this.episode();

    if (!episode || this.isExporting()) {
      return;
    }

    this.isExporting.set(true);
    this.exportMessage.set('Export en cours...');
    this.exportSubscription?.unsubscribe();
    this.exportSubscription = this.travelApiService.exportEpisode(episode.travelId, episode.id).subscribe({
      next: (exportedEpisode) => {
        this.episode.set({ ...episode, ...exportedEpisode });
        this.isExporting.set(false);
        this.exportMessage.set(
          exportedEpisode.exportStatus === 'ready' ? 'Export prêt.' : 'L’export a échoué.',
        );
      },
      error: () => {
        this.isExporting.set(false);
        this.exportMessage.set('L’export a échoué.');
      },
    });
  }

  protected sceneOrder(scene: Scene, index: number): number {
    return scene.order ?? index + 1;
  }

  protected sceneType(scene: Scene): string {
    return scene.type ?? 'souvenir';
  }

  protected sceneStatus(scene: Scene): string {
    const status = scene.generationStatus as SceneGenerationStatus | undefined;
    return status && status in STATUS_LABELS ? STATUS_LABELS[status] : (scene.generationStatus ?? 'Generee');
  }

  protected sceneVoiceOver(scene: Scene): string {
    return scene.voiceOverText ?? scene.description ?? '';
  }

  protected isScenesLoading(): boolean {
    return false;
  }

  protected scenesErrorMessage(): string {
    return '';
  }

  protected isAiReconstructed(scene: Scene): boolean {
    return Boolean(scene.isAiReconstructed);
  }

  protected sceneImage(scene: Scene, episode: Episode): string | null {
    return scene.imageUrl ?? scene.photoUrl ?? episode.videoStill ?? null;
  }

  protected sceneImageAlt(scene: Scene, index: number): string {
    return scene.title || `Scene ${index + 1}`;
  }

  protected sceneMeta(scene: Scene, index: number): string {
    return `#${this.sceneOrder(scene, index)} - ${scene.timecode} - ${this.sceneType(scene)}`;
  }

  protected sceneStatusClass(scene: Scene, _episode: Episode): string {
    return `scene-status scene-status--${scene.generationStatus ?? 'completed'}`;
  }

  protected sceneStatusLabel(scene: Scene, _episode: Episode): string {
    return this.sceneStatus(scene);
  }

  private loadTravel(travelId: string): void {
    this.travelSubscription?.unsubscribe();
    this.travelSubscription = this.travelApiService.getTravel(travelId).subscribe({
      next: (travel) => this.travel.set(travel),
      error: () => this.travel.set(null),
    });
  }

  private copyShareLink(shareToken: string | undefined): void {
    if (!shareToken || typeof window === 'undefined') {
      this.shareMessage.set('Lien généré.');
      return;
    }

    const link = `${window.location.origin}/shared/episode/${shareToken}`;
    navigator.clipboard?.writeText(link)
      .then(() => this.shareMessage.set('Lien copié.'))
      .catch(() => this.shareMessage.set(link));
  }
}
