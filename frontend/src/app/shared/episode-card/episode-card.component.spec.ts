import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { mockEpisodes } from '../../mock-data/travel.mock';
import { EpisodeCardComponent } from './episode-card.component';

@Component({ template: '' })
class EmptyPageComponent {}

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
    fixture.componentRef.setInput('episode', mockEpisodes[0]);
    fixture.detectChanges();
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the episode input data and metadata', () => {
    const element = fixture.nativeElement as HTMLElement;
    const image = element.querySelector('img') as HTMLImageElement;

    expect(element.textContent).toContain(mockEpisodes[0].title);
    expect(element.textContent).toContain(mockEpisodes[0].summary);
    expect(element.textContent).toContain(mockEpisodes[0].location);
    expect(element.textContent).toContain(mockEpisodes[0].duration);
    expect(element.textContent).toContain('S1:E1');
    expect(image.getAttribute('src')).toBe(mockEpisodes[0].coverImage);
    expect(image.getAttribute('alt')).toBe(mockEpisodes[0].title);
  });

  it('renders the reading progress when an episode has started', () => {
    const progress = fixture.nativeElement.querySelector('[aria-label="Progression de lecture"] span') as HTMLElement;

    expect(progress.style.width).toBe(`${mockEpisodes[0].progress}%`);
  });

  it('emits the selected episode when the card is clicked', async () => {
    const selectedSpy = vi.fn();
    fixture.componentInstance.selected.subscribe(selectedSpy);

    (fixture.nativeElement.querySelector('a') as HTMLAnchorElement).click();
    await fixture.whenStable();

    expect(selectedSpy).toHaveBeenCalledWith(mockEpisodes[0]);
  });

  it('uses the compact player variant when requested', () => {
    fixture.componentRef.setInput('compact', true);
    fixture.componentRef.setInput('destination', 'player');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const link = element.querySelector('a') as HTMLAnchorElement;
    const image = element.querySelector('img') as HTMLImageElement;

    expect(link.classList).toContain('episode-card--compact');
    expect(link.getAttribute('href')).toBe('/player/1');
    expect(image.getAttribute('src')).toBe(mockEpisodes[0].videoStill);
    expect(element.textContent).toContain(mockEpisodes[0].date);
    expect(element.textContent).not.toContain(mockEpisodes[0].summary);
  });
});
