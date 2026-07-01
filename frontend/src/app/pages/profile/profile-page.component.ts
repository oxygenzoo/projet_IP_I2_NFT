import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subscription, catchError, forkJoin, of } from 'rxjs';

import { AuthService, ConnectedProfile } from '../../services/auth.service';
import { TravelApiService } from '../../services/travel-api.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-profile-page',
  imports: [ReactiveFormsModule, RouterLink, AppLogoComponent],
  templateUrl: './profile-page.component.html',
})
export class ProfilePageComponent implements OnInit, OnDestroy {
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly travelApiService = inject(TravelApiService);
  private statsSubscription?: Subscription;

  protected readonly profile = signal<ConnectedProfile | null>(null);
  protected readonly isLoading = signal(true);
  protected readonly isSaving = signal(false);
  protected readonly feedbackMessage = signal('');
  protected readonly errorMessage = signal('');
  protected readonly avatarError = signal('');
  protected readonly travelCount = signal(0);
  protected readonly episodeCount = signal(0);
  protected readonly photoCount = signal(0);
  protected readonly avatarUrl = computed(() => this.profileForm.controls.avatarUrl.value || this.profile()?.avatarUrl || '');
  protected readonly initials = computed(() => this.profile()?.initials ?? 'NF');

  protected readonly profileForm = this.formBuilder.nonNullable.group({
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    avatarUrl: [''],
  });

  ngOnInit(): void {
    void this.loadProfile();
    this.loadStats();
  }

  ngOnDestroy(): void {
    this.statsSubscription?.unsubscribe();
  }

  protected async saveProfile(): Promise<void> {
    if (this.profileForm.invalid || this.isSaving()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.feedbackMessage.set('');
    this.errorMessage.set('');

    const result = await this.authService.updateProfile(this.profileForm.getRawValue());

    if (!result.success) {
      this.errorMessage.set(result.message ?? 'Impossible de mettre à jour le profil.');
      this.isSaving.set(false);
      return;
    }

    await this.loadProfile();
    this.feedbackMessage.set(result.message ?? 'Profil mis à jour.');
    this.isSaving.set(false);
  }

  protected async logout(): Promise<void> {
    await this.authService.logout();
    await this.router.navigate(['/login']);
  }

  protected onAvatarSelection(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    this.avatarError.set('');

    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      this.avatarError.set('Choisissez une image.');
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      this.avatarError.set('La photo doit faire moins de 2 Mo.');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      if (typeof reader.result === 'string') {
        this.profileForm.patchValue({ avatarUrl: reader.result });
        this.feedbackMessage.set('');
      }
    };
    reader.onerror = () => this.avatarError.set("Impossible de lire l'image.");
    reader.readAsDataURL(file);
  }

  private async loadProfile(): Promise<void> {
    const profile = await this.authService.getCurrentProfile();

    if (!profile) {
      await this.router.navigate(['/login']);
      return;
    }

    this.profile.set(profile);
    this.profileForm.patchValue({
      fullName: profile.name,
      email: profile.email,
      avatarUrl: profile.avatarUrl ?? '',
    });
    this.isLoading.set(false);
  }

  private loadStats(): void {
    this.statsSubscription?.unsubscribe();
    this.statsSubscription = forkJoin({
      travels: this.travelApiService.getTravels(),
      episodes: this.travelApiService.getEpisodes().pipe(catchError(() => of([]))),
    }).subscribe({
      next: ({ travels, episodes }) => {
        this.travelCount.set(travels.length);
        this.episodeCount.set(episodes.length);
        this.photoCount.set(travels.reduce((total, travel) => total + travel.photoCount, 0));
      },
      error: () => {
        this.travelCount.set(0);
        this.episodeCount.set(0);
        this.photoCount.set(0);
      },
    });
  }
}
