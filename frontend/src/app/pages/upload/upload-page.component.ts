import { Component, computed, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CloudImportService } from '../../services/cloud-import.service';
import { CreationStateService } from '../../services/creation-state.service';
import { TravelDraftService } from '../../services/travel-draft.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';
import { Photo } from '../../models/travel.models';

@Component({
  selector: 'app-upload-page',
  imports: [AppLogoComponent],
  templateUrl: './upload-page.component.html',
  styleUrl: './upload-page.component.scss',
})
export class UploadPageComponent {
  static readonly MAX_FILE_SIZE = 10 * 1024 * 1024;

  private readonly draft = inject(TravelDraftService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly cloudImport = inject(CloudImportService);
  private readonly creationState = inject(CreationStateService);
  private readonly travelApi = inject(TravelApiService);

  protected readonly selectedImages = this.draft.selectedImages;
  protected readonly consentGiven = this.draft.consentRgpd;
  protected readonly uploadedPhotos = signal<Photo[]>([]);
  protected readonly selectedCount = computed(() => Math.max(this.selectedImages().length, this.uploadedPhotos().length));
  protected readonly errors = signal<string[]>([]);
  protected readonly isDragging = signal(false);
  protected readonly isCloudImporting = signal(false);
  protected readonly isUploading = signal(false);
  protected readonly isCreatingLink = signal(false);
  protected readonly contributionLink = signal('');
  protected readonly contributionFeedback = signal('');

  constructor() {
    if (this.route.snapshot.queryParamMap.get('reason') === 'missing-photos') {
      this.errors.set([
        'Les photos doivent être sélectionnées dans cette session. Réimportez vos images pour relancer la génération.',
      ]);
    }
    this.creationState.startUpload(this.draft.travelId());
    this.loadPersistedPhotos();
  }

  protected onFileSelection(event: Event): void {
    const input = event.target as HTMLInputElement;
    void this.addFiles(input.files);
    input.value = '';
  }

  protected onICloudSelection(event: Event): void {
    this.onFileSelection(event);
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
    void this.addFiles(event.dataTransfer?.files ?? null);
  }

  protected removeImage(index: number): void {
    this.draft.removeImage(index);
  }

  protected updateConsent(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.draft.setConsentRgpd(input.checked);
    this.errors.set([]);
  }

  protected async importFromDrive(): Promise<void> {
    await this.importCloudFiles(() => this.cloudImport.importFromDrive());
  }

  protected async importFromGooglePhotos(): Promise<void> {
    await this.importCloudFiles(() => this.cloudImport.importFromGooglePhotos());
  }

  protected openICloudPicker(input: HTMLInputElement): void {
    input.click();
  }

  protected async createContributionLink(): Promise<void> {
    this.isCreatingLink.set(true);
    this.errors.set([]);
    try {
      const travelId = await this.ensureDraftTravel();
      const link = await firstValueFrom(this.travelApi.createContributionLink(travelId));
      this.contributionLink.set(this.publicContributionUrl(link));
      this.contributionFeedback.set('Lien prêt à partager.');
    } catch (error) {
      this.errors.set([this.errorText(error, 'Impossible de créer le lien de contribution.')]);
    } finally {
      this.isCreatingLink.set(false);
    }
  }

  protected async copyContributionLink(): Promise<void> {
    const link = this.contributionLink();
    if (!link) {
      return;
    }

    try {
      await navigator.clipboard.writeText(link);
      this.contributionFeedback.set('Lien copié.');
    } catch {
      this.contributionFeedback.set('Copie indisponible : sélectionnez le lien manuellement.');
    }
  }

  protected async continueToPreferences(): Promise<void> {
    if (selectedGuard(this.selectedCount(), this.consentGiven())) {
      this.errors.set(['Ajoutez au moins une photo et acceptez le traitement privé avant de continuer.']);
      return;
    }

    try {
      const travelId = await this.ensureDraftTravel();
      this.creationState.markPreferences(travelId);
      await this.router.navigate(['/preferences', travelId]);
    } catch (error) {
      this.errors.set([this.errorText(error, 'Impossible de préparer votre souvenir.')]);
    }
  }

  protected formatFileSize(size: number): string {
    if (size < 1024 * 1024) {
      return `${Math.max(1, Math.round(size / 1024))} Ko`;
    }

    return `${(size / (1024 * 1024)).toFixed(1)} Mo`;
  }

  private async addFiles(fileList: FileList | null): Promise<void> {
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
    if (validFiles.length) {
      this.isUploading.set(true);
      try {
        const travelId = await this.ensureDraftTravel();
        const uploaded = await Promise.all(
          validFiles.map((file) => firstValueFrom(this.travelApi.uploadPhoto(travelId, file, this.consentGiven()))),
        );
        this.uploadedPhotos.update((current) => [...current, ...uploaded]);
      } catch (error) {
        this.errors.update((current) => [
          ...current,
          this.errorText(error, "Certaines photos n'ont pas pu être envoyées au backend."),
        ]);
      } finally {
        this.isUploading.set(false);
      }
    }
  }

  private async importCloudFiles(loader: () => Promise<File[]>): Promise<void> {
    if (!this.consentGiven()) {
      this.errors.set(["Vous devez accepter le traitement privé de vos photos avant l'import."]);
      return;
    }

    this.isCloudImporting.set(true);
    this.errors.set([]);
    try {
      const files = await loader();
      await this.addFiles(this.filesToFileList(files));
    } catch (error) {
      this.errors.set([this.errorText(error, 'Import impossible pour le moment.')]);
    } finally {
      this.isCloudImporting.set(false);
    }
  }

  private async ensureDraftTravel(): Promise<string> {
    const existingTravelId = this.draft.travelId();
    if (existingTravelId) {
      return existingTravelId;
    }

    const travel = await firstValueFrom(this.travelApi.createTravel({
      title: 'Nouveau souvenir',
      destination: '',
      description: 'Création en cours',
    }));
    this.draft.setTravelId(travel.id);
    this.creationState.startUpload(travel.id);
    return travel.id;
  }

  private loadPersistedPhotos(): void {
    const travelId = this.draft.travelId() ?? this.creationState.creation()?.travelId ?? null;
    if (!travelId) {
      return;
    }

    this.draft.setTravelId(travelId);
    this.travelApi.getPhotos(travelId).subscribe({
      next: (photos) => this.uploadedPhotos.set(photos),
      error: () => this.uploadedPhotos.set([]),
    });
  }

  private filesToFileList(files: File[]): FileList {
    const dataTransfer = new DataTransfer();
    for (const file of files) {
      dataTransfer.items.add(file);
    }
    return dataTransfer.files;
  }

  private errorText(error: unknown, fallback: string): string {
    if (error instanceof HttpErrorResponse) {
      const payload = error.error;
      if (payload && typeof payload === 'object' && 'message' in payload && typeof payload.message === 'string') {
        return payload.message;
      }
      if (typeof payload === 'string' && payload.trim()) {
        return payload;
      }
    }
    return error instanceof Error && error.message ? error.message : fallback;
  }

  private publicContributionUrl(link: { token: string; url: string }): string {
    if (typeof window === 'undefined') {
      return link.url;
    }

    return `${window.location.origin}/contribute/${link.token}`;
  }
}

function selectedGuard(selectedCount: number, consentGiven: boolean): boolean {
  return selectedCount === 0 || !consentGiven;
}
