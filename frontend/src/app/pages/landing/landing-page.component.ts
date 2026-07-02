import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { I18nService } from '../../services/i18n.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';
import { LanguageSelectComponent } from '../../shared/language-select/language-select.component';

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink, AppLogoComponent, LanguageSelectComponent],
  templateUrl: './landing-page.component.html',
})
export class LandingPageComponent {
  protected readonly i18n = inject(I18nService);

  protected readonly heroImage =
    'url(https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1800&q=85)';

  protected readonly benefits = computed(() => [
    {
      title: this.i18n.literal('Retrouver ses souvenirs'),
      text: this.i18n.literal('Les photos cachées dans la galerie deviennent un souvenir clair, rangé par voyage.'),
    },
    {
      title: this.i18n.literal('Partager avec ses proches'),
      text: this.i18n.literal('Un format souvenir simple à montrer, plus vivant qu’un album photo interminable.'),
    },
    {
      title: this.i18n.literal('Revivre son voyage'),
      text: this.i18n.literal('La narration, le rythme et les scènes recréent l’émotion du départ.'),
    },
  ]);
}
