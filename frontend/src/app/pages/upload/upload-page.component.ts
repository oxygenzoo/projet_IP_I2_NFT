import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CloudImportService } from '../../services/cloud-import.service';
import { CreationStateService } from '../../services/creation-state.service';
import { TravelDraftService } from '../../services/travel-draft.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

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
  protected readonly selectedCount = computed(() => this.selectedImages().length);
  protected readonly errors = signal<string[]>([]);
  protected readonly isDragging = signal(false);
  protected readonly isCloudImporting = signal(false);
  protected readonly isCreatingLink = signal(false);
  protected readonly contributionLink = signal('');

  constructor() {
    if (this.route.snapshot.queryParamMap.get('reason') === 'missing-photos') {
      this.errors.set([
        'Les photos doivent être sélectionnées dans cette session. Réimportez vos images pour relancer la génération.',
      ]);
    }
    this.creationState.startUpload(this.draft.travelId());
  }

  protected onFileSelection(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(input.files);
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
    if (!this.consentGiven()) {
      this.errors.set(['Acceptez le traitement privé des photos avant de créer un lien.']);
      return;
    }

    this.isCreatingLink.set(true);
    this.errors.set([]);
    try {
      const travelId = await this.ensureDraftTravel();
      const link = await firstValueFrom(this.travelApi.createContributionLink(travelId));
      this.contributionLink.set(link.url);
    } catch (error) {
      this.errors.set([this.errorText(error, 'Impossible de créer le lien de contribution.')]);
    } finally {
      this.isCreatingLink.set(false);
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
    if (validFiles.length) {
      this.creationState.startUpload(this.draft.travelId());
      void this.ensureDraftTravel();
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
      this.addFiles(this.filesToFileList(files));
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
      description: 'Creation en cours',
    }));
    this.draft.setTravelId(travel.id);
    this.creationState.startUpload(travel.id);
    return travel.id;
  }

  private filesToFileList(files: File[]): FileList {
    const dataTransfer = new DataTransfer();
    for (const file of files) {
      dataTransfer.items.add(file);
    }
    return dataTransfer.files;
  }

  private errorText(error: unknown, fallback: string): string {
    return error instanceof Error && error.message ? error.message : fallback;
  }
}

function selectedGuard(selectedCount: number, consentGiven: boolean): boolean {
  return selectedCount === 0 || !consentGiven;
}
