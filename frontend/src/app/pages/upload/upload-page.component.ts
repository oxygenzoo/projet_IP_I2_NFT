import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterLink } from '@angular/router';
import { TravelDraftService } from '../../services/travel-draft.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-upload-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './upload-page.component.html',
  styleUrl: './upload-page.component.scss',
})
export class UploadPageComponent {
  static readonly MAX_FILE_SIZE = 10 * 1024 * 1024;

  private readonly draft = inject(TravelDraftService);
  private readonly route = inject(ActivatedRoute);

  protected readonly selectedImages = this.draft.selectedImages;
  protected readonly consentGiven = this.draft.consentRgpd;
  protected readonly selectedCount = computed(() => this.selectedImages().length);
  protected readonly errors = signal<string[]>([]);
  protected readonly isDragging = signal(false);

  constructor() {
    if (this.route.snapshot.queryParamMap.get('reason') === 'missing-photos') {
      this.errors.set([
        'Les photos doivent etre selectionnees dans cette session. Reimportez vos images pour relancer la generation.',
      ]);
    }
  }

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
    this.draft.removeImage(index);
  }

  protected updateConsent(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.draft.setConsentRgpd(input.checked);
    this.errors.set([]);
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

  private addFiles(fileList: FileList | null): void {
    if (!fileList?.length) {
      return;
    }

    if (!this.consentGiven()) {
      this.errors.set(['Vous devez accepter le traitement privé de vos photos avant l’import.']);
      return;
    }

    const validFiles: File[] = [];
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

      validFiles.push(file);
    }

    this.errors.set(validationErrors);
    this.draft.addFiles(validFiles);
  }
}
