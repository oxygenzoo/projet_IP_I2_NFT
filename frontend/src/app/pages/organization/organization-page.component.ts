import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';

@Component({
  selector: 'app-organization-page',
  imports: [RouterLink, AppLogoComponent],
  templateUrl: './organization-page.component.html',
})
export class OrganizationPageComponent {
  protected readonly audiences = [
    ['B2C', 'Des familles qui veulent transformer un voyage en souvenir privé.'],
    ['Groupes', 'Des voyages collectifs avec photos centralisées et souvenir final partagé.'],
    ['Colonies', 'Un récit simple pour rassurer les parents et valoriser les activités.'],
    ['EHPAD', 'Des souvenirs accompagnés, lisibles et partageables avec les proches.'],
    ['Associations', 'Un format clair pour raconter sorties, missions et événements.'],
    ['Organisateurs', 'Un livrable émotionnel à proposer après chaque séjour.'],
  ];
}
