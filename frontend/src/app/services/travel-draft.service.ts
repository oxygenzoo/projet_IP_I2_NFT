import { Injectable, signal } from '@angular/core';

import { DraftImage, TravelPreferences } from '../models/generation.models';

@Injectable({ providedIn: 'root' })
export class TravelDraftService {
  readonly selectedImages = signal<DraftImage[]>([]);
  readonly preferences = signal<TravelPreferences>(this.readJson<TravelPreferences>('nft.draft.preferences', {}));
  readonly travelId = signal<string | null>(this.readText('nft.draft.travelId'));
  readonly consentRgpd = signal(this.readText('nft.draft.consentRgpd') === 'true');

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
    this.writeJson('nft.draft.preferences', preferences);
  }

  setTravelId(travelId: string | null): void {
    this.travelId.set(travelId);
    this.writeText('nft.draft.travelId', travelId);
  }

  setConsentRgpd(consent: boolean): void {
    this.consentRgpd.set(consent);
    this.writeText('nft.draft.consentRgpd', String(consent));
  }

  clear(): void {
    for (const image of this.selectedImages()) {
      URL.revokeObjectURL(image.previewUrl);
    }

    this.selectedImages.set([]);
    this.preferences.set({});
    this.travelId.set(null);
    this.consentRgpd.set(false);
    this.remove('nft.draft.preferences');
    this.remove('nft.draft.travelId');
    this.remove('nft.draft.consentRgpd');
  }

  private readText(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private readJson<T>(key: string, fallback: T): T {
    try {
      return JSON.parse(localStorage.getItem(key) ?? 'null') ?? fallback;
    } catch {
      return fallback;
    }
  }

  private writeText(key: string, value: string | null): void {
    try {
      if (value === null) {
        localStorage.removeItem(key);
        return;
      }
      localStorage.setItem(key, value);
    } catch {
      // Local storage can be unavailable during SSR or private browsing.
    }
  }

  private writeJson(key: string, value: unknown): void {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // Local storage can be unavailable during SSR or private browsing.
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // Local storage can be unavailable during SSR or private browsing.
    }
  }
}
