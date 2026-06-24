import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
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

  protected readonly selected = signal<Record<string, string>>({
    style: 'Cinematographique',
    people: 'Tout le monde',
    moments: 'Paysages',
    tone: 'Inspirant',
  });
  protected readonly selectedCount = computed(() => this.draft.selectedImages().length);
  protected readonly errorMessage = signal('');

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
  }
}
