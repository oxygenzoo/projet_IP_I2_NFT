import { Injectable, signal } from '@angular/core';

import { DraftImage, TravelPreferences } from '../models/generation.models';

@Injectable({ providedIn: 'root' })
export class TravelDraftService {
  readonly selectedImages = signal<DraftImage[]>([]);
  readonly preferences = signal<TravelPreferences>({});
  readonly travelId = signal<string | null>(null);

  addFiles(files: File[]): void {
    const images = files.map((file) => ({
      file,
      previewUrl: URL.createObjectURL(file),
    }));

    this.selectedImages.update((current) => [...current, ...images]);
  }

  removeImage(index: number): void {
    const images = this.selectedImages();
    const image = images[index];

    if (image) {
      URL.revokeObjectURL(image.previewUrl);
    }

    this.selectedImages.set(images.filter((_, imageIndex) => imageIndex !== index));
  }

  setPreferences(preferences: TravelPreferences): void {
    this.preferences.set({ ...preferences });
  }

  setTravelId(travelId: string | null): void {
    this.travelId.set(travelId);
  }

  clear(): void {
    for (const image of this.selectedImages()) {
      URL.revokeObjectURL(image.previewUrl);
    }

    this.selectedImages.set([]);
    this.preferences.set({});
    this.travelId.set(null);
  }
}
