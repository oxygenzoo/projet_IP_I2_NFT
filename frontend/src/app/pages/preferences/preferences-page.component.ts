import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { TravelPreferences } from '../../models/generation.models';
import { PreferenceApiService } from '../../services/preference-api.service';
import { TravelDraftService } from '../../services/travel-draft.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

interface PreferenceQuestion {
  id: string;
  title: string;
  options: string[];
}

@Component({
  selector: 'app-preferences-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './preferences-page.component.html',
})
export class PreferencesPageComponent {
  private readonly draft = inject(TravelDraftService);
  private readonly router = inject(Router);
export class PreferencesPageComponent implements OnInit, OnDestroy {
  private readonly draft = inject(TravelDraftService);
  private readonly preferenceApi = inject(PreferenceApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private loadSubscription?: Subscription;
  private saveSubscription?: Subscription;

  protected readonly questions: PreferenceQuestion[] = [
    {
      id: 'style',
      title: 'Quel style souhaitez-vous ?',
      options: ['Documentaire', 'Emotionnel', 'Cinematographique', 'Drole'],
    },
    {
      id: 'people',
      title: 'Quelles personnes mettre en avant ?',
      options: ['Famille', 'Amis', 'Couple', 'Tout le monde'],
    },
    {
      id: 'moments',
      title: 'Quels moments sont les plus importants ?',
      options: ['Paysages', 'Rencontres', 'Activites', 'Culture'],
    },
    {
      id: 'tone',
      title: 'Ton general',
      options: ['Nostalgique', 'Inspirant', 'Fun', 'Aventure'],
    },
  ];

  protected readonly selected = signal<TravelPreferences>({
    style: 'Cinematographique',
    people: 'Tout le monde',
    moments: 'Paysages',
    tone: 'Inspirant',
  });
  protected readonly selectedCount = computed(() => this.draft.selectedImages().length);
  protected readonly errorMessage = signal('');
  protected readonly isSaving = signal(false);

  ngOnInit(): void {
    const travelId = this.resolveTravelId();
    const draftPreferences = this.draft.preferences();

    if (Object.keys(draftPreferences).length) {
      this.applyPreferences(draftPreferences);
    }

    if (!travelId) {
      return;
    }

    this.draft.setTravelId(travelId);
    this.loadSubscription = this.preferenceApi.getPreferences(travelId).subscribe({
      next: (preferences) => {
        const savedPreferences = this.questionPreferencesFrom(preferences);
        this.applyPreferences(savedPreferences);
        this.draft.setPreferences(savedPreferences);
      },
      error: (error: HttpErrorResponse) => {
        if (error.status !== 404) {
          this.errorMessage.set('Impossible de recharger les préférences enregistrées.');
        }
      },
    });
  }

  ngOnDestroy(): void {
    this.loadSubscription?.unsubscribe();
    this.saveSubscription?.unsubscribe();
  }

  protected select(questionId: string, option: string): void {
    this.selected.update((current) => ({
      ...current,
      [questionId]: option,
    }));
  }

  protected isSelected(questionId: string, option: string): boolean {
    return this.selected()[questionId] === option;
  }

  protected selectedSummary(): string {
    const current = this.selected();

    return `${current['style']} · ${current['people']} · ${current['moments']} · ${current['tone']}`;
  }

  protected startGeneration(): void {
    if (this.selectedCount() === 0) {
      this.errorMessage.set('Ajoutez au moins une photo avant de lancer la génération.');
      return;
    }

    this.draft.setPreferences(this.selected());
    this.router.navigate(['/generating']);
    const preferences = this.currentPreferences();

    if (!this.isQuestionnaireComplete(preferences)) {
      this.errorMessage.set('Répondez à toutes les questions avant de lancer la génération.');
      return;
    }

    this.draft.setPreferences(preferences);

    const travelId = this.resolveTravelId();

    if (!travelId) {
      this.router.navigate(['/generating']);
      return;
    }

    this.errorMessage.set('');
    this.isSaving.set(true);
    this.saveSubscription?.unsubscribe();
    this.saveSubscription = this.preferenceApi.savePreferences(travelId, preferences).subscribe({
      next: (savedPreferences) => {
        const questionnairePreferences = this.questionPreferencesFrom(savedPreferences);
        this.applyPreferences(questionnairePreferences);
        this.draft.setPreferences(questionnairePreferences);
        this.isSaving.set(false);
        this.router.navigate(['/generating']);
      },
      error: () => {
        this.isSaving.set(false);
        this.errorMessage.set('La sauvegarde des préférences a échoué. Réessayez dans un instant.');
      },
    });
  }

  private resolveTravelId(): string | null {
    return this.route.snapshot.paramMap.get('travelId')
      ?? this.route.snapshot.queryParamMap.get('travelId')
      ?? this.draft.travelId();
  }

  private currentPreferences(): TravelPreferences {
    const current = this.selected();
    return this.questionPreferencesFrom(current);
  }

  private questionPreferencesFrom(preferences: TravelPreferences): TravelPreferences {
    return Object.fromEntries(
      this.questions.map((question) => [question.id, preferences[question.id]?.trim() ?? '']),
    );
  }

  private isQuestionnaireComplete(preferences: TravelPreferences): boolean {
    return this.questions.every((question) => Boolean(preferences[question.id]));
  }

  private applyPreferences(preferences: TravelPreferences): void {
    this.selected.update((current) => ({
      ...current,
      style: preferences['style'] ?? current['style'],
      people: preferences['people'] ?? current['people'],
      moments: preferences['moments'] ?? current['moments'],
      tone: preferences['tone'] ?? current['tone'],
    }));
  }
}
