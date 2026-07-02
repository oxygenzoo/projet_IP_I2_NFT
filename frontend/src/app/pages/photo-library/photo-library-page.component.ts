import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';

import { API_URL } from '../../config/api.config';
import { Photo, Travel } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

interface TravelPhotos {
  travel: Travel;
  photos: Photo[];
}

@Component({
  selector: 'app-photo-library-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './photo-library-page.component.html',
})
export class PhotoLibraryPageComponent implements OnInit {
  private readonly travelApiService = inject(TravelApiService);
  private readonly apiUrl = inject(API_URL);

  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal('');
  protected readonly groups = signal<TravelPhotos[]>([]);

  ngOnInit(): void {
    this.loadPhotos();
  }

  protected loadPhotos(): void {
    this.isLoading.set(true);
    this.errorMessage.set('');

    this.travelApiService.getTravels().pipe(catchError(() => of([]))).subscribe((travels) => {
      if (!travels.length) {
        this.groups.set([]);
        this.isLoading.set(false);
        return;
      }

      forkJoin(
        travels.map((travel) =>
          this.travelApiService.getPhotos(travel.id).pipe(catchError(() => of([] as Photo[]))),
        ),
      ).subscribe({
        next: (photosByTravel) => {
          this.groups.set(
            travels
              .map((travel, index) => ({ travel, photos: photosByTravel[index] ?? [] }))
              .filter((group) => group.photos.length > 0),
          );
          this.isLoading.set(false);
        },
        error: () => {
          this.errorMessage.set('Bibliothèque photos indisponible.');
          this.isLoading.set(false);
        },
      });
    });
  }

  protected imageUrl(photo: Photo): string {
    return photo.imageUrl.startsWith('/') ? `${this.apiUrl}${photo.imageUrl}` : photo.imageUrl;
  }

  protected deletePhoto(group: TravelPhotos, photo: Photo): void {
    this.travelApiService.deletePhoto(group.travel.id, photo.id).subscribe({
      next: () => {
        this.groups.update((groups) =>
          groups
            .map((current) =>
              current.travel.id === group.travel.id
                ? { ...current, photos: current.photos.filter((item) => item.id !== photo.id) }
                : current,
            )
            .filter((current) => current.photos.length > 0),
        );
      },
      error: () => this.errorMessage.set('Suppression impossible pour le moment.'),
    });
  }
}
