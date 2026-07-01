import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Episode } from '../../models/travel.models';
import { EpisodeCardComponent } from './episode-card.component';

@Component({ template: '' })
class EmptyPageComponent {}

const episodeFixture: Episode = {
  id: 'episode-1',
  travelId: 'travel-1',
  seasonNumber: 1,
  episodeNumber: 1,
  title: 'Arrivee a Lisbonne',
  subtitle: 'Premier soir',
  summary: 'Une arrivee au coucher du soleil.',
  duration: '3 min',
  location: 'Lisbonne',
  date: '2026-06-12',
  photoCount: 12,
  coverImage: 'https://cdn.example.com/cover.jpg',
  videoStill: 'https://cdn.example.com/still.jpg',
  progress: 42,
  remaining: '1 min',
  scenes: [],
};

describe('EpisodeCardComponent', () => {
  let fixture: ComponentFixture<EpisodeCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EpisodeCardComponent],
      providers: [
        provideRouter([
          { path: 'episode/:id', component: EmptyPageComponent },
          { path: 'player/:id', component: EmptyPageComponent },
        ]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(EpisodeCardComponent);
    fixture.componentRef.setInput('episode', episodeFixture);
    fixture.detectChanges();
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the episode input data and metadata', () => {
    const element = fixture.nativeElement as HTMLElement;
    const image = element.querySelector('img') as HTMLImageElement;

    expect(element.textContent).toContain(episodeFixture.title);
    expect(element.textContent).toContain(episodeFixture.summary);
    expect(element.textContent).toContain(episodeFixture.location);
    expect(element.textContent).toContain(episodeFixture.duration);
    expect(element.textContent).toContain('S1:E1');
    expect(image.getAttribute('src')).toBe(episodeFixture.coverImage);
    expect(image.getAttribute('alt')).toBe(episodeFixture.title);
  });

  it('renders the reading progress when an episode has started', () => {
    const progress = fixture.nativeElement.querySelector('[aria-label="Progression de lecture"] span') as HTMLElement;

    expect(progress.style.width).toBe(`${episodeFixture.progress}%`);
  });

  it('emits the selected episode when the card is clicked', async () => {
    const selectedSpy = vi.fn();
    fixture.componentInstance.selected.subscribe(selectedSpy);

    (fixture.nativeElement.querySelector('a') as HTMLAnchorElement).click();
    await fixture.whenStable();

    expect(selectedSpy).toHaveBeenCalledWith(episodeFixture);
  });

  it('uses the compact player variant when requested', () => {
    fixture.componentRef.setInput('compact', true);
    fixture.componentRef.setInput('destination', 'player');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const link = element.querySelector('a') as HTMLAnchorElement;
    const image = element.querySelector('img') as HTMLImageElement;

    expect(link.classList).toContain('episode-card--compact');
    expect(link.getAttribute('href')).toBe('/player/episode-1');
    expect(image.getAttribute('src')).toBe(episodeFixture.videoStill);
    expect(element.textContent).toContain(episodeFixture.date);
    expect(element.textContent).not.toContain(episodeFixture.summary);
  });
});
