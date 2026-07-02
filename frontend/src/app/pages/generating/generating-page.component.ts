import { isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { GenerationResponse } from '../../models/generation.models';
import { GenerationApiService } from '../../services/generation-api.service';
import { TravelDraftService } from '../../services/travel-draft.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-generating-page',
  imports: [AppLogoComponent, RouterLink],
  templateUrl: './generating-page.component.html',
})
export class GeneratingPageComponent implements OnInit, OnDestroy {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly draft = inject(TravelDraftService);
  private readonly generationApi = inject(GenerationApiService);
  private readonly router = inject(Router);
  private intervalId?: ReturnType<typeof setInterval>;
  private redirectId?: ReturnType<typeof setTimeout>;
  private generationSubscription?: Subscription;

  protected readonly steps = [
    'Analyse des photos',
    'Sélection des meilleurs souvenirs',
    'Création du récit',
    'Assemblage du souvenir',
    'Finalisation',
  ];
  protected readonly confettiPieces = Array.from({ length: 30 }, (_, index) => index + 1);

  protected readonly percent = signal(7);
  protected readonly activeIndex = signal(0);
  protected readonly completed = signal<string[]>([]);
  protected readonly errorMessage = signal('');
  protected readonly result = signal<GenerationResponse | null>(null);
  protected readonly isGenerating = signal(false);

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      this.percent.set(100);
      this.completed.set(this.steps);
      this.activeIndex.set(this.steps.length - 1);
      return;
    }

    this.startGeneration();
  }

  ngOnDestroy(): void {
    this.clearTimers();
    this.generationSubscription?.unsubscribe();
  }

  protected retry(): void {
    this.startGeneration();
  }

  protected firstEpisodeTitle(): string {
    return this.result()?.script?.episodes?.[0]?.episode_titre ?? 'Souvenir généré';
  }

  protected videoUrl(): string {
    return this.result()?.videos?.[0] ?? '';
  }

  protected episodeCount(): number {
    return this.result()?.script?.nb_episodes ?? 1;
  }

  protected sceneCount(): number {
    return this.result()?.script?.episodes?.[0]?.scenes?.length ?? 0;
  }

  protected photoCount(): number {
    return this.draft.selectedImages().length;
  }

  protected estimatedDuration(): string {
    const seconds = Math.max(20, 10 + this.sceneCount() * 4);
    return seconds < 60 ? `${seconds} s` : `${Math.max(1, Math.round(seconds / 60))} min`;
  }

  private startGeneration(): void {
    const images = this.draft.selectedImages();

    if (images.length === 0) {
      this.router.navigate(['/upload'], {
        queryParams: { reason: 'missing-photos' },
        replaceUrl: true,
      });
      this.percent.set(0);
      this.completed.set([]);
      this.activeIndex.set(0);
      return;
    }

    this.errorMessage.set('');
    this.result.set(null);
    this.isGenerating.set(true);
    this.percent.set(7);
    this.activeIndex.set(0);
    this.completed.set([]);
    this.clearTimers();

    this.intervalId = setInterval(() => {
      const nextPercent = Math.min(this.percent() + 4, 92);
      const nextStepIndex = Math.min(Math.floor(nextPercent / 22), this.steps.length - 1);

      this.percent.set(nextPercent);
      this.activeIndex.set(nextStepIndex);
      this.completed.set(this.steps.slice(0, nextStepIndex));
    }, 700);

    this.generationSubscription?.unsubscribe();
    this.generationSubscription = this.generationApi.createEpisode(
      images,
      this.draft.preferences(),
      this.draft.travelId(),
    ).subscribe({
      next: (response) => {
        this.result.set(response);
        this.percent.set(100);
        this.activeIndex.set(this.steps.length - 1);
        this.completed.set(this.steps);
        this.isGenerating.set(false);
        this.clearTimers();
      },
      error: (error: Error) => {
        this.errorMessage.set(error.message);
        this.isGenerating.set(false);
        this.clearTimers();
      },
    });
  }

  private clearTimers(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = undefined;
    }

    if (this.redirectId) {
      clearTimeout(this.redirectId);
      this.redirectId = undefined;
    }
  }
}
