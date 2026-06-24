import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Travel } from '../../models/travel.models';
import { MockTravelService } from '../../services/mock-travel.service';
import { AppLogoComponent } from '../../shared/app-logo/app-logo.component';
import { EpisodeCardComponent } from '../../shared/episode-card/episode-card.component';

@Component({
  selector: 'app-home-page',
  imports: [RouterLink, AppLogoComponent, EpisodeCardComponent],
  templateUrl: './home-page.component.html',
})
export class HomePageComponent {
  private readonly travelService = inject(MockTravelService);

  protected readonly user = this.travelService.getUser();
  protected readonly featured = this.travelService.getFeaturedTravel();
  protected readonly featuredEpisode = this.featured.episodes[0];
  protected readonly continueEpisode = this.travelService.getContinueWatchingEpisode();
  protected readonly travels = this.travelService.getTravels();
  protected readonly recentEpisodes = this.travelService.getEpisodes().slice(0, 6);
  protected readonly recommendedTravels = this.travels.filter((travel) => !travel.featured);
  protected readonly heroImage = `url(${this.featured.heroImage})`;

  protected travelPlayerLink(travel: Travel): string[] {
    return ['/episode', travel.episodes[0]?.id ?? '1'];
  }
}
