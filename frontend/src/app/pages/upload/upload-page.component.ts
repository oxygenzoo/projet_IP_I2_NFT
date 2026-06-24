import { Component, computed, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

interface SelectedImage {
  file: File;
  previewUrl: string;
}

@Component({
  selector: 'app-upload-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './upload-page.component.html',
  styleUrl: './upload-page.component.scss',
})
export class UploadPageComponent implements OnDestroy {
  static readonly MAX_FILE_SIZE = 10 * 1024 * 1024;

  protected readonly selectedImages = signal<SelectedImage[]>([]);
  protected readonly selectedCount = computed(() => this.selectedImages().length);
  protected readonly errors = signal<string[]>([]);
  protected readonly isDragging = signal(false);

  protected onFileSelection(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(input.files);
    input.value = '';
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(true);
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragging.set(false);
    this.addFiles(event.dataTransfer?.files ?? null);
  }

  protected removeImage(index: number): void {
    const images = this.selectedImages();
    URL.revokeObjectURL(images[index].previewUrl);
    this.selectedImages.set(images.filter((_, imageIndex) => imageIndex !== index));
  }

  protected preferencesLink(): string | string[] {
    const travelId = this.draft.travelId();

    return travelId ? ['/preferences', travelId] : '/preferences';
  }

  protected formatFileSize(size: number): string {
    if (size < 1024 * 1024) {
      return `${Math.max(1, Math.round(size / 1024))} Ko`;
    }

    return `${(size / (1024 * 1024)).toFixed(1)} Mo`;
  }

  ngOnDestroy(): void {
    for (const image of this.selectedImages()) {
      URL.revokeObjectURL(image.previewUrl);
    }
  }

  private addFiles(fileList: FileList | null): void {
    if (!fileList?.length) {
      return;
    }

    const validImages: SelectedImage[] = [];
    const validationErrors: string[] = [];

    for (const file of Array.from(fileList)) {
      if (!file.type.startsWith('image/')) {
        validationErrors.push(`${file.name} : seuls les fichiers images sont acceptés.`);
        continue;
      }

      if (file.size > UploadPageComponent.MAX_FILE_SIZE) {
        validationErrors.push(`${file.name} : le fichier dépasse la limite de 10 Mo.`);
        continue;
      }

      validImages.push({
        file,
        previewUrl: URL.createObjectURL(file),
      });
    }

    this.errors.set(validationErrors);
    this.selectedImages.update((images) => [...images, ...validImages]);
  }
}
