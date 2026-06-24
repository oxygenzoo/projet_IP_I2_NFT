import { Component, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-upload-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './upload-page.component.html',
})
export class UploadPageComponent {
  protected readonly selectedCount = signal(0);
  protected readonly selectedFileNames = signal<string[]>([]);
  protected readonly errorMessage = signal('');

  private readonly acceptedTypes = new Set(['image/jpeg', 'image/png', 'image/heic', 'image/heif']);
  private readonly maxFileSize = 10 * 1024 * 1024;

  protected selectPhotos(input: HTMLInputElement): void {
    input.click();
  }

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);

    this.errorMessage.set('');
    this.selectedCount.set(0);
    this.selectedFileNames.set([]);

    if (files.length === 0) {
      this.errorMessage.set('Selectionnez au moins une photo.');
      return;
    }

    const invalidType = files.find((file) => !this.acceptedTypes.has(file.type));
    if (invalidType) {
      this.errorMessage.set('Format non pris en charge. Utilisez JPG, PNG ou HEIC.');
      input.value = '';
      return;
    }

    const oversizedFile = files.find((file) => file.size > this.maxFileSize);
    if (oversizedFile) {
      this.errorMessage.set('Chaque photo doit peser moins de 10 Mo.');
      input.value = '';
      return;
    }

    this.selectedCount.set(files.length);
    this.selectedFileNames.set(files.map((file) => file.name));
  }
}
