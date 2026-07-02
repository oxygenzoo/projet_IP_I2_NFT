import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-contribute-page',
  imports: [AppLogoComponent],
  templateUrl: './contribute-page.component.html',
  styleUrl: '../upload/upload-page.component.scss',
})
export class ContributePageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly travelApi = inject(TravelApiService);

  protected readonly title = signal('Souvenir');
  protected readonly selectedFiles = signal<File[]>([]);
  protected readonly isLoading = signal(true);
  protected readonly isUploading = signal(false);
  protected readonly errorMessage = signal('');
  protected readonly successMessage = signal('');

  ngOnInit(): void {
    void this.loadContribution();
  }

  protected onFileSelection(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFiles.set(Array.from(input.files ?? []).filter((file) => file.type.startsWith('image/')));
    input.value = '';
    this.errorMessage.set('');
    this.successMessage.set('');
  }

  protected async upload(): Promise<void> {
    const token = this.token();
    const files = this.selectedFiles();
    if (!token || !files.length || this.isUploading()) {
      return;
    }

    this.isUploading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');
    try {
      for (const file of files) {
        await firstValueFrom(this.travelApi.uploadContributionPhoto(token, file, true));
      }
      this.successMessage.set(`${files.length} photo${files.length > 1 ? 's' : ''} ajoutée${files.length > 1 ? 's' : ''}.`);
      this.selectedFiles.set([]);
    } catch {
      this.errorMessage.set("L'envoi a échoué. Vérifiez le lien ou réessayez dans un instant.");
    } finally {
      this.isUploading.set(false);
    }
  }

  private async loadContribution(): Promise<void> {
    const token = this.token();
    if (!token) {
      this.errorMessage.set('Lien de contribution invalide.');
      this.isLoading.set(false);
      return;
    }

    try {
      const info = await firstValueFrom(this.travelApi.getContributionInfo(token));
      this.title.set(info.title);
    } catch {
      this.errorMessage.set('Ce lien de contribution est invalide ou expiré.');
    } finally {
      this.isLoading.set(false);
    }
  }

  private token(): string {
    return this.route.snapshot.paramMap.get('token') ?? '';
  }
}
