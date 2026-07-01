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
  private sceneSubscription?: Subscription;

  protected readonly episode = signal<Episode | null>(null);
  protected readonly travel = signal<Travel | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly scenes = signal<Scene[]>([]);
  protected readonly isScenesLoading = signal(false);
  protected readonly scenesErrorMessage = signal('');
  protected readonly coverImage = computed(() => `url(${this.episode()?.coverImage ?? ''})`);
  protected readonly sortedScenes = computed(() => this.sortScenes(this.scenes()));

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
        this.loadScenes(episode);
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
    this.sceneSubscription?.unsubscribe();
  }

  protected sceneImage(scene: Scene, episode: Episode): string {
    if (this.isAiReconstructed(scene) && !scene.imageUrl) {
      return '';
    }

    return scene.imageUrl || episode.coverImage || episode.videoStill || '';
  }

  protected isAiReconstructed(scene: Scene): boolean {
    return scene.isAiReconstructed === true;
  }

  protected sceneImageAlt(scene: Scene, index: number): string {
    if (this.isAiReconstructed(scene)) {
      return `Image reconstituee par IA pour la scene ${index + 1}`;
    }

    return scene.title || `Photo de la scene ${index + 1}`;
  }

  protected sceneVoiceOver(scene: Scene): string {
    return (
      scene.voiceOverText ||
      scene.voiceoverText ||
      scene.narrationText ||
      scene.description ||
      'Texte de voix-off indisponible pour cette scène.'
    );
  }

  protected sceneStatus(scene: Scene, episode: Episode): SceneGenerationStatus {
    if (scene.status) {
      return scene.status;
    }

    if (episode.progress >= 100) {
      return 'completed';
    }

    if (episode.progress > 0) {
      return 'generating';
    }

    return 'pending';
  }

  protected sceneStatusLabel(scene: Scene, episode: Episode): string {
    return STATUS_LABELS[this.sceneStatus(scene, episode)];
  }

  protected sceneStatusClass(scene: Scene, episode: Episode): string {
    return `scene-status scene-status--${this.sceneStatus(scene, episode)}`;
  }

  protected sceneDuration(scene: Scene): string {
    if (scene.duration === undefined || scene.duration === null || scene.duration === '') {
      return '';
    }

    return typeof scene.duration === 'number' ? `${scene.duration} s` : scene.duration;
  }

  protected sceneMeta(scene: Scene, index: number): string {
    const parts = [`Scene ${index + 1}`];

    if (scene.timecode) {
      parts.push(scene.timecode);
    }

    const duration = this.sceneDuration(scene);
    if (duration) {
      parts.push(duration);
    }

    return parts.join(' - ');
  }

  private loadScenes(episode: Episode): void {
    this.sceneSubscription?.unsubscribe();
    this.isScenesLoading.set(true);
    this.scenesErrorMessage.set('');

    if (Array.isArray(episode.scenes)) {
      this.scenes.set(episode.scenes);
      this.isScenesLoading.set(false);
      return;
    }

    this.sceneSubscription = this.travelApiService.getEpisodeScenes(episode.id).subscribe({
      next: (scenes) => {
        this.scenes.set(scenes);
        this.isScenesLoading.set(false);
      },
      error: () => {
        this.scenes.set([]);
        this.scenesErrorMessage.set("Impossible de charger la timeline de l'épisode.");
        this.isScenesLoading.set(false);
      },
    });
  }

  private sortScenes(scenes: Scene[]): Scene[] {
    return scenes
      .map((scene, index) => ({ scene, index }))
      .sort((left, right) => {
        const leftOrder = this.sceneOrder(left.scene);
        const rightOrder = this.sceneOrder(right.scene);

        if (leftOrder !== null && rightOrder !== null && leftOrder !== rightOrder) {
          return leftOrder - rightOrder;
        }

        if (leftOrder !== null && rightOrder === null) {
          return -1;
        }

        if (leftOrder === null && rightOrder !== null) {
          return 1;
        }

        const leftDate = this.createdAtTime(left.scene);
        const rightDate = this.createdAtTime(right.scene);

        if (leftDate !== null && rightDate !== null && leftDate !== rightDate) {
          return leftDate - rightDate;
        }

        return left.index - right.index;
      })
      .map(({ scene }) => scene);
  }

  private sceneOrder(scene: Scene): number | null {
    const order = scene.order ?? scene.sceneOrder ?? scene.position;
    const numericOrder = typeof order === 'string' ? Number(order) : order;
    return typeof numericOrder === 'number' && Number.isFinite(numericOrder) ? numericOrder : null;
  }

  private createdAtTime(scene: Scene): number | null {
    if (!scene.createdAt) {
      return null;
    }

    const timestamp = Date.parse(scene.createdAt);
    return Number.isNaN(timestamp) ? null : timestamp;
  }

  private loadTravel(travelId: string): void {
    this.travelSubscription?.unsubscribe();
    this.travelSubscription = this.travelApiService.getTravel(travelId).subscribe({
      next: (travel) => this.travel.set(travel),
      error: () => this.travel.set(null),
    });
  }
}
