import { Injectable, computed, signal } from '@angular/core';

export type AppLanguage = 'fr' | 'en' | 'es' | 'pt';

const TRANSLATIONS: Record<AppLanguage, Record<string, string>> = {
  fr: {
    createMemory: 'Créer un souvenir',
    creationInProgress: 'Création en cours',
    language: 'Langue',
    save: 'Enregistrer',
    saving: 'Sauvegarde...',
  },
  en: {
    createMemory: 'Create a memory',
    creationInProgress: 'Creation in progress',
    language: 'Language',
    save: 'Save',
    saving: 'Saving...',
  },
  es: {
    createMemory: 'Crear un recuerdo',
    creationInProgress: 'Creación en curso',
    language: 'Idioma',
    save: 'Guardar',
    saving: 'Guardando...',
  },
  pt: {
    createMemory: 'Criar uma lembrança',
    creationInProgress: 'Criação em curso',
    language: 'Idioma',
    save: 'Guardar',
    saving: 'A guardar...',
  },
};

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly storageKey = 'nft.language';
  readonly language = signal<AppLanguage>(this.readLanguage());
  readonly labels = computed(() => TRANSLATIONS[this.language()]);

  setLanguage(language: string): AppLanguage {
    const next = this.normalize(language);
    this.language.set(next);
    localStorage.setItem(this.storageKey, next);
    document.documentElement.lang = next;
    return next;
  }

  t(key: string): string {
    return this.labels()[key] ?? TRANSLATIONS.fr[key] ?? key;
  }

  private readLanguage(): AppLanguage {
    return this.normalize(localStorage.getItem(this.storageKey) ?? 'fr');
  }

  private normalize(language: string): AppLanguage {
    return ['fr', 'en', 'es', 'pt'].includes(language) ? language as AppLanguage : 'fr';
  }
}
