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

  it('renders AI reconstructed scenes with a badge, prompt, and dedicated placeholder', () => {
    const episode = episodeWithScenes([
      {
        id: 'normal-scene',
        title: 'Photo originale',
        order: 1,
        imageUrl: '/real-photo.jpg',
        voiceOverText: 'Une vraie photo importee.',
        status: 'completed',
      },
      {
        id: 'ai-scene',
        title: 'Moment manquant',
        order: 2,
        imageUrl: null,
        voiceOverText: "Aucune photo n'a capture ce moment.",
        status: 'completed',
        isAiReconstructed: true,
        aiPrompt: 'Reconstituer une scene de coucher de soleil a Rome avec ambiance cinematographique.',
      },
    ]);

    render(episode);

    const element = fixture.nativeElement as HTMLElement;
    const aiCard = element.querySelector('.scene-card--ai') as HTMLElement;
    const normalCard = element.querySelector('.scene-card:not(.scene-card--ai)') as HTMLElement;

    expect(aiCard).toBeTruthy();
    expect(aiCard.textContent).toContain('Reconstitu\u00e9 par IA');
    expect(aiCard.textContent).toContain('Image g\u00e9n\u00e9r\u00e9e / reconstitu\u00e9e par IA');
    expect(aiCard.textContent).toContain('Cette sc\u00e8ne est une reconstitution IA, pas une photo originale.');
    expect(aiCard.textContent).toContain('Prompt IA : Reconstituer une scene de coucher de soleil a Rome');
    expect(aiCard.querySelector('img')).toBeNull();
    expect(normalCard.textContent).not.toContain('Reconstitu\u00e9 par IA');
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
