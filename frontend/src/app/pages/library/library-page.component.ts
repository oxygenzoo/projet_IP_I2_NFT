import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { Episode, Travel } from '../../models/travel.models';
import { CreationStateService } from '../../services/creation-state.service';
import { I18nService } from '../../services/i18n.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';
import { EpisodeCardComponent } from '../../shared/episode-card/episode-card.component';

interface TravelMemories {
  travel: Travel;
  memories: Episode[];
}

@Component({
  selector: 'app-library-page',
  imports: [RouterLink, AppLogoComponent, EpisodeCardComponent],
  templateUrl: './library-page.component.html',
})
export class LibraryPageComponent implements OnInit {
  private readonly travelApiService = inject(TravelApiService);
  protected readonly creationState = inject(CreationStateService);
  protected readonly i18n = inject(I18nService);

  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly travels = signal<Travel[]>([]);
  protected readonly episodes = signal<Episode[]>([]);
  protected readonly groupedMemories = computed<TravelMemories[]>(() =>
    this.travels()
      .map((travel) => ({
        travel,
        memories: this.episodes().filter((episode) => episode.travelId === travel.id),
      }))
      .filter((group) => group.memories.length > 0),
  );
  protected readonly createButtonLabel = computed(() =>
    this.creationState.isActive() ? this.i18n.t('creationInProgress') : this.i18n.t('createMemory'),
  );

  ngOnInit(): void {
    this.loadLibrary();
  }

  protected loadLibrary(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    forkJoin({
      travels: this.travelApiService.getTravels().pipe(catchError(() => of([]))),
      episodes: this.travelApiService.getEpisodes().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ travels, episodes }) => {
        this.travels.set(travels);
        this.episodes.set(episodes);
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Bibliothèque indisponible pour le moment.');
        this.isLoading.set(false);
      },
    });
  }
}
