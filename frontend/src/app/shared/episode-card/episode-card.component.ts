import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Episode } from '../../models/travel.models';

@Component({
  selector: 'app-episode-card',
  imports: [RouterLink],
  templateUrl: './episode-card.component.html',
})
export class EpisodeCardComponent {
  readonly episode = input.required<Episode>();
  readonly compact = input(false);
  readonly destination = input<'detail' | 'player'>('detail');
  readonly selected = output<Episode>();

  protected get link(): string[] {
    const target = this.destination() === 'player' || Boolean(this.episode().videoUrl) ? '/player' : '/episode';
    return [target, this.episode().id];
  }

  protected selectEpisode(): void {
    this.selected.emit(this.episode());
  }
}
