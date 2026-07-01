import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';

import { API_URL } from '../../config/api.config';
import { Episode, Travel } from '../../models/travel.models';
import { EpisodeDetailPageComponent } from './episode-detail-page.component';

describe('EpisodeDetailPageComponent', () => {
  let fixture: ComponentFixture<EpisodeDetailPageComponent>;
  let httpTesting: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EpisodeDetailPageComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_URL, useValue: 'http://api.test' },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              paramMap: convertToParamMap({ id: 'episode-1' }),
            },
          },
        },
      ],
    }).compileComponents();

    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('affiche les scenes dans le bon ordre avec les donnees API', () => {
    createComponent();
    flushEpisode({
      ...episode(),
      scenes: [
        scene('scene-2', 2, 'Deuxieme', 'souvenir'),
        scene('scene-1', 1, 'Intro', 'intro'),
      ],
    });
    flushTravel();
    fixture.detectChanges();

    const titles = [...fixture.nativeElement.querySelectorAll('.scene-card h3')].map((title) =>
      title.textContent.trim(),
    );

    expect(titles).toEqual(['Intro', 'Deuxieme']);
    expect(fixture.nativeElement.textContent).toContain('#1');
    expect(fixture.nativeElement.textContent).toContain('Voix off Intro');
  });

  it('affiche un etat vide sans scene', () => {
    createComponent();
    flushEpisode({ ...episode(), scenes: [] });
    flushTravel();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Aucune scene');
  });

  it('affiche clairement une scene IA reconstruite', () => {
    createComponent();
    flushEpisode({
      ...episode(),
      scenes: [
        {
          ...scene('scene-ai', 1, 'Souvenir manquant', 'transition'),
          imageUrl: null,
          isAiReconstructed: true,
          aiPrompt: 'Prompt fictif Ubud',
          generationStatus: 'ai_reconstructed',
        },
      ],
    });
    flushTravel();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Reconstitué par IA');
    expect(fixture.nativeElement.textContent).toContain('Prompt fictif Ubud');
    expect(fixture.nativeElement.querySelector('.scene-ai-placeholder')).toBeTruthy();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(EpisodeDetailPageComponent);
    fixture.detectChanges();
  }

  function flushEpisode(response: Episode): void {
    httpTesting.expectOne('http://api.test/api/episodes/episode-1').flush(response);
  }

  function flushTravel(response: Travel = travel()): void {
    httpTesting.expectOne('http://api.test/api/travels/bali-2025').flush(response);
  }

  function scene(id: string, order: number, title: string, type: Episode['scenes'][number]['type']) {
    return {
      id,
      order,
      title,
      timecode: '00:00',
      voiceOverText: `Voix off ${title}`,
      type,
      generationStatus: 'generated',
      imageUrl: 'https://example.test/photo.jpg',
      isAiReconstructed: false,
      aiPrompt: null,
    };
  }

  function episode(): Episode {
    return {
      id: 'episode-1',
      travelId: 'bali-2025',
      seasonNumber: 1,
      episodeNumber: 1,
      title: 'Arrivee',
      subtitle: 'Demo',
      summary: 'Synopsis demo',
      duration: '18 min',
      location: 'Bali',
      date: '12 mai 2025',
      photoCount: 10,
      coverImage: 'https://example.test/cover.jpg',
      videoStill: 'https://example.test/still.jpg',
      progress: 0,
      remaining: '18 min restantes',
      keyMoments: ['arrivee'],
      scenes: [],
    };
  }

  function travel(): Travel {
    return {
      id: 'bali-2025',
      title: 'Bali 2025',
      destination: 'Bali',
      country: 'Indonesie',
      year: 2025,
      tagline: 'Demo',
      description: 'Demo',
      coverImage: 'https://example.test/cover.jpg',
      heroImage: 'https://example.test/hero.jpg',
      posterImage: 'https://example.test/poster.jpg',
      duration: '39 min',
      episodeCount: 1,
      photoCount: 10,
      progress: 0,
      remaining: '',
      featured: true,
      moodTags: ['Demo'],
      episodes: [],
    };
  }
});
