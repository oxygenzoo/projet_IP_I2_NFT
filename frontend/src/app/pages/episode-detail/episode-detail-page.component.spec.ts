import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { mockEpisodes, mockTravels } from '../../mock-data/travel.mock';
import { Episode, Scene } from '../../models/travel.models';
import { TravelApiService } from '../../services/travel-api.service';
import { EpisodeDetailPageComponent } from './episode-detail-page.component';

@Component({ template: '' })
class EmptyPageComponent {}

describe('EpisodeDetailPageComponent', () => {
  let fixture: ComponentFixture<EpisodeDetailPageComponent>;
  let travelApiService: {
    getEpisode: ReturnType<typeof vi.fn>;
    getTravel: ReturnType<typeof vi.fn>;
    getEpisodeScenes: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    travelApiService = {
      getEpisode: vi.fn(),
      getTravel: vi.fn().mockReturnValue(of(mockTravels[0])),
      getEpisodeScenes: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [EpisodeDetailPageComponent],
      providers: [
        provideRouter([
          { path: 'home', component: EmptyPageComponent },
          { path: 'player/:id', component: EmptyPageComponent },
        ]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: {
                get: () => 'episode-1',
              },
            },
          },
        },
        { provide: TravelApiService, useValue: travelApiService },
      ],
    }).compileComponents();
  });

  it('renders generated scenes in ascending order with voice-over and status', () => {
    const episode = episodeWithScenes([
      {
        id: 'scene-2',
        title: 'Second plan',
        order: 2,
        imageUrl: '/second.jpg',
        voiceOverText: 'La voix-off de la deuxieme scene.',
        status: 'completed',
        duration: 12,
      },
      {
        id: 'scene-1',
        title: 'Premier plan',
        order: 1,
        imageUrl: '/first.jpg',
        voiceOverText: 'La voix-off de la premiere scene.',
        status: 'pending',
        duration: 8,
      },
    ]);

    render(episode);

    const element = fixture.nativeElement as HTMLElement;
    const titles = Array.from(element.querySelectorAll('.scene-card h3')).map((title) => title.textContent?.trim());

    expect(titles).toEqual(['Premier plan', 'Second plan']);
    expect(element.textContent).toContain('La voix-off de la premiere scene.');
    expect(element.textContent).toContain('En attente');
    expect(element.textContent).toContain('Générée');
    expect(element.textContent).toContain('8 s');
  });

  it('falls back to the episode image and legacy description fields', () => {
    const episode = {
      ...episodeWithScenes([
        {
          id: 'legacy-scene',
          title: 'Souvenir archive',
          description: 'Ancien texte narratif conserve.',
        },
      ]),
      coverImage: '/episode-cover.jpg',
      videoStill: '',
    };

    render(episode);

    const element = fixture.nativeElement as HTMLElement;
    const image = element.querySelector('.scene-card img') as HTMLImageElement;

    expect(image.getAttribute('src')).toBe('/episode-cover.jpg');
    expect(element.textContent).toContain('Ancien texte narratif conserve.');
  });

  it('renders an empty timeline message when the episode has no scenes', () => {
    render(episodeWithScenes([]));

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune scène générée pour cet épisode.');
  });

  it('keeps the episode visible when a separate scenes API call fails', () => {
    const episodeWithoutEmbeddedScenes = { ...mockEpisodes[0], id: 'episode-without-scenes' } as Episode;
    delete (episodeWithoutEmbeddedScenes as Partial<Episode>).scenes;
    travelApiService.getEpisodeScenes.mockReturnValue(throwError(() => new Error('API unavailable')));

    render(episodeWithoutEmbeddedScenes);

    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(episodeWithoutEmbeddedScenes.title);
    expect(element.textContent).toContain("Impossible de charger la timeline de l'épisode.");
  });

  it('shows a timeline loading state while scenes are being requested', () => {
    const episodeWithoutEmbeddedScenes = { ...mockEpisodes[0], id: 'episode-with-pending-scenes' } as Episode;
    const scenesRequest = new Subject<Scene[]>();
    delete (episodeWithoutEmbeddedScenes as Partial<Episode>).scenes;
    travelApiService.getEpisodeScenes.mockReturnValue(scenesRequest.asObservable());

    render(episodeWithoutEmbeddedScenes);

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Chargement de la timeline...');

    scenesRequest.next([]);
    scenesRequest.complete();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune scène générée pour cet épisode.');
  });

  function render(episode: Episode): void {
    travelApiService.getEpisode.mockReturnValue(of(episode));
    fixture = TestBed.createComponent(EpisodeDetailPageComponent);
    fixture.detectChanges();
  }

  function episodeWithScenes(scenes: Scene[]): Episode {
    return {
      ...mockEpisodes[0],
      id: 'episode-1',
      progress: 0,
      scenes,
    };
  }
});
