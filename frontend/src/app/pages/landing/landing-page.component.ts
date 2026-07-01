import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-landing-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './landing-page.component.html',
})
export class LandingPageComponent {
  protected readonly heroImage =
    'url(https://images.unsplash.com/photo-1537996194471-e657df975ab4?auto=format&fit=crop&w=1800&q=85)';

  protected readonly benefits = [
    {
      title: 'Retrouver ses souvenirs',
      text: 'Les photos cachées dans la galerie deviennent un souvenir clair, rangé par voyage.',
    },
    {
      title: 'Partager avec ses proches',
      text: 'Un format souvenir simple à montrer, plus vivant qu’un album photo interminable.',
    },
    {
      title: 'Revivre son voyage',
      text: 'La narration, le rythme et les scènes recréent l’émotion du départ.',
    },
  ];
}
