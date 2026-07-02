import { isPlatformBrowser } from '@angular/common';
import { Component, OnDestroy, OnInit, PLATFORM_ID, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { GenerationResponse } from '../../models/generation.models';
import { CreationStateService } from '../../services/creation-state.service';
import { SubscriptionQuotaService } from '../../services/subscription-quota.service';
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
  private readonly creationState = inject(CreationStateService);
  private readonly quota = inject(SubscriptionQuotaService);
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
  protected readonly result = signal<GenerationResponse | null>(this.creationState.creation()?.result ?? null);
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
    const episode = this.result()?.script?.episodes?.[0];
    const sceneSeconds = (episode?.scenes ?? [])
      .slice(0, 6)
      .reduce((total, scene) => total + Math.min(Number(scene.duree_secondes ?? 4), 4), 0);
    const seconds = Math.round(2.5 + this.textCardSeconds(episode?.intro) + sceneSeconds + this.textCardSeconds(episode?.outro));

    return this.formatDuration(seconds);
  }

  private textCardSeconds(text: string | undefined): number {
    if (!text?.trim()) {
      return 0;
    }

    return Math.max(1, Math.ceil(text.trim().length / 58)) * 1.8;
  }

  private formatDuration(seconds: number): string {
    if (seconds < 60) {
      return `${seconds} s`;
    }

    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return remainingSeconds ? `${minutes} min ${remainingSeconds} s` : `${minutes} min`;
  }

  private startGeneration(): void {
    const existingCreation = this.creationState.creation();
    if (existingCreation?.status === 'done') {
      this.result.set(existingCreation.result ?? {
        job_id: existingCreation.id ?? 'creation',
        status: 'done',
        message: 'Souvenir termine.',
        selection_report: {},
        script: { nb_episodes: 1, episodes: [{ episode_titre: 'Souvenir termine', scenes: [] }] },
        videos: existingCreation.resultVideoUrl ? [existingCreation.resultVideoUrl] : [],
        workdir: '',
      });
      this.percent.set(100);
      this.completed.set(this.steps);
      this.activeIndex.set(this.steps.length - 1);
      return;
    }

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

    if (!this.quota.canGenerateVideo()) {
      this.router.navigate(['/pricing'], {
        queryParams: { reason: 'token-limit' },
        replaceUrl: true,
      });
      this.percent.set(0);
      this.completed.set([]);
      this.activeIndex.set(0);
      return;
    }

    this.errorMessage.set('');
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

    this.creationState.beginGeneration(images, this.draft.preferences(), this.draft.travelId());
    this.generationSubscription?.unsubscribe();
    this.generationSubscription = new Subscription();

    const watchId = setInterval(() => {
      const creation = this.creationState.creation();
      if (creation?.status === 'done') {
        this.quota.consumeVideoToken();
        this.result.set(creation.result ?? null);
        this.percent.set(100);
        this.activeIndex.set(this.steps.length - 1);
        this.completed.set(this.steps);
        this.isGenerating.set(false);
        this.clearTimers();
        clearInterval(watchId);
      }
      if (creation?.status === 'error') {
        this.errorMessage.set(creation.errorMessage ?? 'La génération a échoué.');
        this.isGenerating.set(false);
        this.clearTimers();
        clearInterval(watchId);
      }
    }, 500);
    this.generationSubscription.add(() => clearInterval(watchId));
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
