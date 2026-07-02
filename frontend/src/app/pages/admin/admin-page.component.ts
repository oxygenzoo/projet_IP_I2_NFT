import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { Subscription, catchError, forkJoin, of } from 'rxjs';

import { Episode, Travel } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-admin-page',
  imports: [AppLogoComponent],
  templateUrl: './admin-page.component.html',
})
export class AdminPageComponent implements OnInit, OnDestroy {
  private readonly travelApiService = inject(TravelApiService);
  private dashboardSubscription?: Subscription;

  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly travels = signal<Travel[]>([]);
  protected readonly episodes = signal<Episode[]>([]);
  protected readonly totalPhotos = computed(() => this.travels().reduce((total, travel) => total + travel.photoCount, 0));
  protected readonly failedEpisodes = computed(() => this.episodes().filter((episode) => this.hasAiError(episode)).length);

  ngOnInit(): void {
    this.loadDashboard();
  }

  ngOnDestroy(): void {
    this.dashboardSubscription?.unsubscribe();
  }

  protected loadDashboard(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');
    this.dashboardSubscription?.unsubscribe();
    this.dashboardSubscription = forkJoin({
      travels: this.travelApiService.getTravels(),
      episodes: this.travelApiService.getEpisodes().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ travels, episodes }) => {
        this.travels.set(travels);
        this.episodes.set(episodes);
        this.isLoading.set(false);
      },
      error: () => {
        this.travels.set([]);
        this.episodes.set([]);
        this.errorMessage.set("Impossible de charger les donnees d'administration.");
        this.isLoading.set(false);
      },
    });
  }

  protected episodeStatus(episode: Episode): string {
    return episode.status || episode.exportStatus || 'unknown';
  }

  protected photoCountFor(episode: Episode): number {
    return episode.photoCount || episode.scenes?.length || 0;
  }

  protected aiErrorLabel(episode: Episode): string {
    if (!this.hasAiError(episode)) {
      return 'Aucune';
    }

    return episode.exportStatus === 'failed' ? 'Export échoué' : 'Génération échouée';
  }

  private hasAiError(episode: Episode): boolean {
    return episode.status === 'failed'
      || episode.exportStatus === 'failed'
      || (episode.scenes ?? []).some((scene) => scene.generationStatus === 'failed');
  }
}
