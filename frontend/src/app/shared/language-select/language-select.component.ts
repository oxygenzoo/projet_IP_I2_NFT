import { Component, inject } from '@angular/core';
import { AppLanguage, I18nService } from '../../services/i18n.service';

@Component({
  selector: 'app-language-select',
  imports: [],
  templateUrl: './language-select.component.html',
  styleUrl: './language-select.component.scss',
})
export class LanguageSelectComponent {
  protected readonly i18n = inject(I18nService);

  protected readonly languages: Array<{ value: AppLanguage; label: string }> = [
    { value: 'fr', label: 'Français' },
    { value: 'en', label: 'English' },
    { value: 'es', label: 'Español' },
    { value: 'pt', label: 'Português' },
  ];

  protected changeLanguage(event: Event): void {
    const select = event.target as HTMLSelectElement;
    this.i18n.setLanguage(select.value);
  }
}
