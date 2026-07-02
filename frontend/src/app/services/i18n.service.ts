import { Injectable, computed, signal } from '@angular/core';

import { APP_TRANSLATIONS, AppLanguage, LITERAL_TRANSLATIONS, TranslationParams } from '../i18n/translations';

export type { AppLanguage };

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly storageKey = 'nft.language';
  readonly language = signal<AppLanguage>(this.readLanguage());
  readonly labels = computed(() => APP_TRANSLATIONS[this.language()]);

  constructor() {
    if (typeof document !== 'undefined') {
      document.documentElement.lang = this.language();
    }
  }

  setLanguage(language: string): AppLanguage {
    const next = this.normalize(language);
    this.language.set(next);
    localStorage.setItem(this.storageKey, next);
    if (typeof document !== 'undefined') {
      document.documentElement.lang = next;
    }
    return next;
  }

  t(key: string, params: TranslationParams = {}): string {
    return this.interpolate(this.labels()[key] ?? APP_TRANSLATIONS.fr[key] ?? key, params);
  }

  literal(text: string, params: TranslationParams = {}): string {
    const language = this.language();
    if (language === 'fr') {
      return this.interpolate(text, params);
    }
    return this.interpolate(LITERAL_TRANSLATIONS[language][text] ?? text, params);
  }

  plural(count: number, singularKey: string, pluralKey: string): string {
    return this.t(count === 1 ? singularKey : pluralKey);
  }

  private readLanguage(): AppLanguage {
    return this.normalize(localStorage.getItem(this.storageKey) ?? 'fr');
  }

  private normalize(language: string): AppLanguage {
    return ['fr', 'en', 'es', 'pt'].includes(language) ? language as AppLanguage : 'fr';
  }

  private interpolate(template: string, params: TranslationParams): string {
    return template.replace(/\{(\w+)}/g, (_, key: string) => String(params[key] ?? `{${key}}`));
  }
}
