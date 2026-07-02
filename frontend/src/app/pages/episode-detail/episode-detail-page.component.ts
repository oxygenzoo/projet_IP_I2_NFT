import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { API_URL } from '../../config/api.config';
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
  private readonly router = inject(Router);
  private readonly travelApiService = inject(TravelApiService);
  private readonly apiUrl = inject(API_URL);
  private episodeSubscription?: Subscription;
  private travelSubscription?: Subscription;
  private shareSubscription?: Subscription;
  private exportSubscription?: Subscription;
  private favoriteSubscription?: Subscription;
  private deleteSubscription?: Subscription;

  protected readonly episode = signal<Episode | null>(null);
  protected readonly travel = signal<Travel | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly shareMessage = signal('');
  protected readonly exportMessage = signal('');
  protected readonly actionMessage = signal('');
  protected readonly isExporting = signal(false);
  protected readonly isSavingFavorite = signal(false);
  protected readonly isDeleting = signal(false);
  protected readonly coverImage = computed(() => `url(${this.episode()?.coverImage ?? ''})`);
  protected readonly sortedScenes = computed(() =>
    [...(this.episode()?.scenes ?? [])].sort(
      (left, right) => (left.order ?? 0) - (right.order ?? 0),
    ),
  );

  ngOnInit(): void {
    const episodeId = this.route.snapshot.paramMap.get('id');

    if (!episodeId) {
      this.errorMessage.set('Souvenir introuvable.');
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
        this.errorMessage.set("Ce souvenir n'existe pas dans votre espace.");
        this.isLoading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.episodeSubscription?.unsubscribe();
    this.travelSubscription?.unsubscribe();
    this.shareSubscription?.unsubscribe();
    this.exportSubscription?.unsubscribe();
    this.favoriteSubscription?.unsubscribe();
    this.deleteSubscription?.unsubscribe();
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

  protected toggleFavorite(): void {
    const episode = this.episode();

    if (!episode || this.isSavingFavorite()) {
      return;
    }

    const nextFavorite = !episode.favorite;
    this.isSavingFavorite.set(true);
    this.actionMessage.set('');
    this.favoriteSubscription?.unsubscribe();
    this.favoriteSubscription = this.travelApiService.updateEpisodeFavorite(
      episode.travelId,
      episode.id,
      nextFavorite,
    ).subscribe({
      next: (updatedEpisode) => {
        this.episode.set({ ...episode, ...updatedEpisode, favorite: nextFavorite });
        this.isSavingFavorite.set(false);
        this.actionMessage.set(nextFavorite ? 'Ajouté aux favoris.' : 'Retiré des favoris.');
      },
      error: () => {
        this.isSavingFavorite.set(false);
        this.actionMessage.set('Impossible de modifier le favori.');
      },
    });
  }

  protected deleteEpisode(): void {
    const episode = this.episode();

    if (!episode || this.isDeleting()) {
      return;
    }

    if (typeof window !== 'undefined' && !window.confirm('Supprimer définitivement ce souvenir ?')) {
      return;
    }

    this.isDeleting.set(true);
    this.actionMessage.set('Suppression en cours...');
    this.deleteSubscription?.unsubscribe();
    this.deleteSubscription = this.travelApiService.deleteEpisode(episode.travelId, episode.id).subscribe({
      next: () => void this.router.navigate(['/library']),
      error: () => {
        this.isDeleting.set(false);
        this.actionMessage.set('Suppression impossible pour le moment.');
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
    return status && status in STATUS_LABELS ? STATUS_LABELS[status] : (scene.generationStatus ?? 'Générée');
  }

  protected sceneVoiceOver(scene: Scene): string {
    return scene.voiceOverText ?? scene.description ?? '';
  }

  protected metadataItems(episode: Episode): string[] {
    return [
      episode.duration,
      episode.location,
      episode.date,
      episode.photoCount > 0 ? `${episode.photoCount} photos` : '',
    ].filter((item): item is string => Boolean(item?.trim()));
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
    return this.assetUrl(scene.imageUrl)
      ?? this.assetUrl(scene.photoUrl)
      ?? this.assetUrl(episode.videoStill)
      ?? this.assetUrl(episode.coverImage)
      ?? this.assetUrl(this.travel()?.posterImage)
      ?? this.assetUrl(this.travel()?.coverImage)
      ?? null;
  }

  protected sceneImageAlt(scene: Scene, index: number): string {
    return scene.title || `Scene ${index + 1}`;
  }

  protected sceneMeta(scene: Scene, index: number): string {
    return [`#${this.sceneOrder(scene, index)}`, scene.timecode, this.sceneType(scene)]
      .filter((item) => Boolean(item?.trim()))
      .join(' - ');
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

  private assetUrl(value: string | null | undefined): string | null {
    const trimmed = value?.trim();

    if (!trimmed) {
      return null;
    }

    return trimmed.startsWith('/') ? `${this.apiUrl}${trimmed}` : trimmed;
  }
}
