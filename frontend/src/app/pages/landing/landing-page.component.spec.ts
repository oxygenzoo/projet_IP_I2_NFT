import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { LandingPageComponent } from './landing-page.component';

@Component({ template: '' })
class EmptyPageComponent {}

describe('LandingPageComponent', () => {
  let fixture: ComponentFixture<LandingPageComponent>;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandingPageComponent],
      providers: [
        provideRouter([
          { path: 'upload', component: EmptyPageComponent },
          { path: 'login', component: EmptyPageComponent },
          { path: 'pricing', component: EmptyPageComponent },
        ]),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LandingPageComponent);
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  it('creates the component', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('renders the main landing content and benefits', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Vos voyages.');
    expect(element.querySelector('.landing-hero')).toBeTruthy();
    expect(element.querySelectorAll('.benefit-card')).toHaveLength(3);
    expect(element.textContent).toContain('Photos');
    expect(element.textContent).toContain('IA');
    expect(element.textContent).toContain('Episode');
  });

  it('shows the primary navigation actions', () => {
    const links = Array.from(fixture.nativeElement.querySelectorAll('a')) as HTMLAnchorElement[];

    expect(links.some((link) => link.textContent?.includes('Tarifs'))).toBe(true);
    expect(links.some((link) => link.textContent?.includes('Se connecter'))).toBe(true);
    expect(links.filter((link) => link.textContent?.includes('Commencer')).length).toBeGreaterThanOrEqual(2);
  });

  it('navigates to the upload flow when the main call to action is clicked', async () => {
    const callToAction = fixture.nativeElement.querySelector('.landing-hero .btn--primary') as HTMLAnchorElement;

    callToAction.click();
    await fixture.whenStable();

    expect(router.url).toBe('/upload');
  });
});
